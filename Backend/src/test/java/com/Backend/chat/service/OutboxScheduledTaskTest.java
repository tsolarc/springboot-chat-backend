package com.Backend.chat.service;

import com.Backend.Chat.service.implementation.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboxScheduledTaskTest {
    @Test
    void recordsWorkerFailureWithoutReplacingTheOriginalException() {
        var publisher = mock(MessageOutboxPublisher.class);
        var audit = mock(ScheduledTaskAuditService.class);
        var failure = new IllegalStateException("database unavailable");
        doThrow(failure).when(publisher).publishReady(any(), any());
        doThrow(new IllegalStateException("audit unavailable")).when(audit).finish(any(), any(), anyLong(), any());
        assertThatThrownBy(new OutboxScheduledTask(publisher, audit)::run).isSameAs(failure);
        verify(audit).finish(any(UUID.class), any(Instant.class), anyLong(), same(failure));
    }

    @Test
    void completesAuditAfterPublisherReturns() {
        var publisher = mock(MessageOutboxPublisher.class);
        var audit = mock(ScheduledTaskAuditService.class);
        new OutboxScheduledTask(publisher, audit).run();
        var order = inOrder(publisher, audit);
        order.verify(publisher).publishReady(any(), any());
        order.verify(audit).finish(any(), any(), anyLong(), isNull());
    }
}
