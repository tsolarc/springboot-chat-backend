package com.Backend.chat.service;

import com.Backend.Chat.entity.Message.MessageOutboxEvent;
import com.Backend.Chat.outbox.*;
import com.Backend.Chat.service.implementation.MessageOutboxPublisher;
import com.Backend.Chat.service.implementation.ScheduledTaskAuditService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MessageOutboxPublisherTest {
    @Test
    void failedTransportIsSettledForRetryAndRecordedInMetrics() throws Exception {
        var store = mock(OutboxStore.class);
        var transport = mock(EventPublisher.class);
        var audit = mock(ScheduledTaskAuditService.class);
        var metrics = new SimpleMeterRegistry();
        var event = new MessageOutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAttempts(1);
        when(store.claimNext()).thenReturn(Optional.of(event), Optional.empty());
        var failure = new IllegalStateException("broker unavailable");
        doThrow(failure).when(transport).publish(event);
        var publisher = new MessageOutboxPublisher(store, transport, new OutboxProperties(), audit, metrics);
        UUID run = UUID.randomUUID();
        publisher.publishReady(run, Instant.now());
        verify(store).settle(eq(run), same(event), same(failure), anyLong());
        assertThat(metrics.counter("messaging.outbox.publications", "outcome", "failure").count()).isEqualTo(1);
    }

    @Test
    void noPendingMessagesDoesNotCreateAuditRows() {
        var store = mock(OutboxStore.class);
        var audit = mock(ScheduledTaskAuditService.class);
        var transport = mock(EventPublisher.class);
        when(store.claimNext()).thenReturn(Optional.empty());
        new MessageOutboxPublisher(store, transport, new OutboxProperties(), audit, new SimpleMeterRegistry())
                .publishReady(UUID.randomUUID(), Instant.now());
        verifyNoInteractions(audit, transport);
    }
}
