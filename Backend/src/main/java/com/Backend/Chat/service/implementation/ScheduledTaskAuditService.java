package com.Backend.Chat.service.implementation;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduledTaskAuditService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID runId, Instant startedAt, Instant finishedAt, long durationMs,
                       List<MessageOutboxPublisher.EventAttempt> attempts, Exception failure) {
        if (attempts.isEmpty() && failure == null) {
            return;
        }
        long failed = attempts.stream().filter(attempt -> !attempt.success()).count();
        String status = failure != null ? "FAILED" : failed > 0 ? "PARTIAL_FAILURE" : "SUCCESS";
        jdbcTemplate.update("""
                INSERT INTO scheduled_task_log
                (id, task_name, status, trigger_type, instance_id, started_at, finished_at,
                 duration_ms, processed_count, success_count, failure_count, error_type, error_message)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, runId, "publish-message-outbox", status, "SCHEDULED",
                System.getenv().getOrDefault("HOSTNAME", "unknown"), Timestamp.from(startedAt),
                Timestamp.from(finishedAt), durationMs, attempts.size(), attempts.size() - failed,
                failed, failure == null ? null : bounded(failure.getClass().getName(), 200),
                failure == null ? null : bounded(failure.getMessage(), 500));

        for (MessageOutboxPublisher.EventAttempt attempt : attempts) {
            jdbcTemplate.update("""
                    INSERT INTO scheduled_task_event_log
                    (task_log_id, event_id, routing_key, origin_user_id, origin_chat_id,
                     origin_message_id, origin_client_message_id, origin_operation, origin_source, attempt_number,
                     status, duration_ms, error_type, error_message)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, runId, attempt.eventId(), attempt.routingKey(), attempt.userId(),
                    attempt.chatId(), attempt.messageId(), attempt.clientMessageId(),
                    attempt.operation(), attempt.source(),
                    attempt.attemptNumber(), attempt.success() ? "SUCCESS" : "FAILED",
                    attempt.durationMs(), bounded(attempt.errorType(), 200),
                    bounded(attempt.errorMessage(), 500));
        }
    }

    private String bounded(String value, int maxLength) {
        return value == null ? null : value.substring(0, Math.min(value.length(), maxLength));
    }
}
