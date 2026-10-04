package com.Backend.chat.service;

import com.Backend.Chat.entity.Message.MessageOutboxEvent;
import com.Backend.Chat.repository.MessageOutboxRepository;
import com.Backend.Chat.service.implementation.MessageOutboxPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageOutboxPublisherTest {

    @Mock MessageOutboxRepository repository;
    @Mock RabbitTemplate rabbitTemplate;
    @InjectMocks MessageOutboxPublisher publisher;

    private MessageOutboxEvent event() {
        MessageOutboxEvent event = new MessageOutboxEvent();
        event.setId(UUID.randomUUID());
        event.setRoutingKey("message.created");
        event.setPayload("{}");
        event.setCreatedAt(Instant.now());
        event.setNextAttemptAt(Instant.now());
        return event;
    }

    @Test
    void confirmedPublishMarksEventComplete() {
        MessageOutboxEvent event = event();
        when(repository.findReadyForUpdate(any(Instant.class), any(Pageable.class))).thenReturn(List.of(event));
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(eq("messaging.events"), eq("message.created"), any(), any(CorrelationData.class));

        publisher.publishReady();

        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getAttempts()).isZero();
    }

    @Test
    void failedPublishSchedulesRetry() {
        MessageOutboxEvent event = event();
        when(repository.findReadyForUpdate(any(Instant.class), any(Pageable.class))).thenReturn(List.of(event));
        doThrow(new AmqpException("broker unavailable"))
                .when(rabbitTemplate).send(anyString(), anyString(), any(), any(CorrelationData.class));

        publisher.publishReady();

        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isAfter(event.getCreatedAt());
    }
}
