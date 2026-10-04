package com.Backend.chat.outbox;

import com.Backend.Chat.entity.Message.MessageOutboxEvent;
import com.Backend.Chat.outbox.OutboxProperties;
import com.Backend.Chat.outbox.RabbitEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RabbitEventPublisherTest {
    @Test
    void requiresBrokerConfirmationAndPropagatesEventIdentity() throws Exception {
        var rabbit = mock(RabbitTemplate.class);
        var event = event();
        doAnswer(call -> {
            Message message = call.getArgument(2);
            assertThat(message.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
            assertThat(message.getMessageProperties().getMessageId()).isEqualTo(event.getId().toString());
            assertThat(message.getMessageProperties().getHeader("requestId").toString()).isEqualTo("request-123");
            CorrelationData correlation = call.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(eq("messaging.events"), eq("message.created"), any(Message.class), any(CorrelationData.class));
        new RabbitEventPublisher(rabbit, new OutboxProperties()).publish(event);
        verify(rabbit).send(eq("messaging.events"), eq("message.created"), any(Message.class), any(CorrelationData.class));
    }

    @Test
    void ackWithReturnedMessageIsAFailure() {
        var rabbit = mock(RabbitTemplate.class);
        doAnswer(call -> {
            CorrelationData correlation = call.getArgument(3);
            correlation.setReturned(new ReturnedMessage(call.getArgument(2), 312, "NO_ROUTE", "messaging.events", "unknown"));
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        assertThatThrownBy(() -> new RabbitEventPublisher(rabbit, new OutboxProperties()).publish(event()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void nackIsAFailure() {
        var rabbit = mock(RabbitTemplate.class);
        doAnswer(call -> {
            CorrelationData correlation = call.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(false, "rejected"));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        assertThatThrownBy(() -> new RabbitEventPublisher(rabbit, new OutboxProperties()).publish(event()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void missingConfirmationTimesOut() {
        var settings = new OutboxProperties();
        settings.setConfirmTimeout(Duration.ofMillis(10));
        assertThatThrownBy(() -> new RabbitEventPublisher(mock(RabbitTemplate.class), settings).publish(event()))
                .isInstanceOf(TimeoutException.class);
    }

    private MessageOutboxEvent event() {
        var event = new MessageOutboxEvent();
        event.setId(UUID.randomUUID());
        event.setRoutingKey("message.created");
        event.setPayload("{}");
        event.setOriginRequestId("request-123");
        return event;
    }
}
