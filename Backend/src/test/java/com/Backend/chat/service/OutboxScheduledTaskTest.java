package com.Backend.chat.service;

import com.Backend.Chat.service.implementation.MessageOutboxPublisher;
import com.Backend.Chat.service.implementation.OutboxScheduledTask;
import com.Backend.Chat.service.implementation.ScheduledTaskAuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxScheduledTaskTest {

    @Mock MessageOutboxPublisher publisher;
    @Mock ScheduledTaskAuditService audit;
    @InjectMocks OutboxScheduledTask task;

    @Test
    void recordsAttemptProvenanceAfterPublishing() {
        var attempt = new MessageOutboxPublisher.EventAttempt(UUID.randomUUID(), "message.created",
                12L, 34L, 56L, UUID.randomUUID(), "MESSAGE_CREATE", "POST /v1/conversations/34/messages",
                1, true, 8L, null, null);
        when(publisher.publishReady()).thenReturn(new MessageOutboxPublisher.PublishResult(List.of(attempt)));

        task.run();

        verify(audit).record(any(UUID.class), any(Instant.class), any(Instant.class), anyLong(),
                eq(List.of(attempt)), isNull());
    }

    @Test
    void recordsWorkerFailureAndPropagatesIt() {
        IllegalStateException failure = new IllegalStateException("database unavailable");
        when(publisher.publishReady()).thenThrow(failure);

        assertThatThrownBy(task::run).isSameAs(failure);

        verify(audit).record(any(UUID.class), any(Instant.class), any(Instant.class), anyLong(),
                eq(List.of()), same(failure));
    }
}
