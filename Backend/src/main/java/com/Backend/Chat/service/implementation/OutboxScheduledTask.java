package com.Backend.Chat.service.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduledTask {

    private final MessageOutboxPublisher publisher;
    private final ScheduledTaskAuditService auditService;

    public void run() {
        UUID runId = UUID.randomUUID();
        Instant startedAt = Instant.now();
        long startedNanos = System.nanoTime();
        try {
            MessageOutboxPublisher.PublishResult result = publisher.publishReady();
            auditService.record(runId, startedAt, Instant.now(), elapsed(startedNanos), result.attempts(), null);
        } catch (Exception failure) {
            try {
                auditService.record(runId, startedAt, Instant.now(), elapsed(startedNanos), List.of(), failure);
            } catch (Exception auditFailure) {
                log.error("Could not audit failed outbox run {}", runId, auditFailure);
            }
            throw failure;
        }
    }

    private long elapsed(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }
}
