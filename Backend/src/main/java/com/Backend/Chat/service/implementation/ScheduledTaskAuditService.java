package com.Backend.Chat.service.implementation;

import com.Backend.Chat.entity.Message.MessageOutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduledTaskAuditService {
    private final JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void start(UUID runId, Instant startedAt) {
        jdbc.update("""
                INSERT INTO scheduled_task_log (id, task_name, status, trigger_type, instance_id, started_at)
                VALUES (?, 'publish-message-outbox', 'RUNNING', 'SCHEDULED', ?, ?)
                """, runId, System.getenv().getOrDefault("HOSTNAME", "process-" + ProcessHandle.current().pid()),
                Timestamp.from(startedAt));
    }

    /** Stored in the same transaction as the outbox transition, so a rollback cannot leave false success. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordAttempt(UUID runId, MessageOutboxEvent event, Exception failure, long durationMs) {
        jdbc.update("""
                INSERT INTO scheduled_task_event_log
                (task_log_id, event_id, routing_key, origin_user_id, origin_chat_id, origin_message_id,
                 origin_client_message_id, origin_operation, origin_source, origin_request_id,
                 attempt_number, status, duration_ms, error_type, error_message)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, runId, event.getId(), event.getRoutingKey(), event.getOriginUserId(), event.getOriginChatId(),
                event.getOriginMessageId(), event.getOriginClientMessageId(), event.getOriginOperation(),
                event.getOriginSource(), event.getOriginRequestId(), event.getAttempts(),
                failure == null ? "SUCCESS" : event.getDeadLetteredAt() != null ? "DEAD_LETTERED" : "FAILED",
                durationMs, failure == null ? null : bounded(failure.getClass().getName(), 200),
                failure == null ? null : bounded(failure.getMessage(), 500));
        jdbc.update("""
                UPDATE scheduled_task_log SET processed_count = processed_count + 1,
                success_count = success_count + ?, failure_count = failure_count + ? WHERE id = ?
                """, failure == null ? 1 : 0, failure == null ? 0 : 1, runId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finish(UUID runId, Instant startedAt, long durationMs, Exception failure) {
        int updated = jdbc.update("""
                UPDATE scheduled_task_log SET finished_at = ?, duration_ms = ?, error_type = ?, error_message = ?,
                status = CASE WHEN ? THEN 'FAILED' WHEN failure_count = 0 THEN 'SUCCESS'
                    WHEN success_count = 0 THEN 'FAILED' ELSE 'PARTIAL_FAILURE' END WHERE id = ?
                """, Timestamp.from(Instant.now()), durationMs,
                failure == null ? null : bounded(failure.getClass().getName(), 200),
                failure == null ? null : bounded(failure.getMessage(), 500), failure != null, runId);
        if (updated == 0 && failure != null) {
            jdbc.update("""
                    INSERT INTO scheduled_task_log
                    (id, task_name, status, trigger_type, started_at, finished_at, duration_ms, error_type, error_message)
                    VALUES (?, 'publish-message-outbox', 'FAILED', 'SCHEDULED', ?, ?, ?, ?, ?)
                    """, runId, Timestamp.from(startedAt), Timestamp.from(Instant.now()), durationMs,
                    bounded(failure.getClass().getName(), 200), bounded(failure.getMessage(), 500));
        }
    }

    private String bounded(String value, int maxLength) {
        return value == null ? null : value.substring(0, Math.min(value.length(), maxLength));
    }
}
