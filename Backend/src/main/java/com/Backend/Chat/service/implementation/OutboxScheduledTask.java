package com.Backend.Chat.service.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
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
        long clock = System.nanoTime();
        Exception failure = null;
        try {
            publisher.publishReady(runId, startedAt);
        } catch (Exception exception) {
            failure = exception;
            throw exception;
        } finally {
            try {
                auditService.finish(runId, startedAt, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - clock), failure);
            } catch (Exception auditFailure) {
                log.error("Could not finish audit runId={}", runId, auditFailure);
                if (failure == null) throw auditFailure;
                failure.addSuppressed(auditFailure);
            }
        }
    }
}
