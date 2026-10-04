package com.Backend.Chat.service.implementation;

import com.Backend.Chat.outbox.EventPublisher;
import com.Backend.Chat.outbox.OutboxProperties;
import com.Backend.Chat.outbox.OutboxStore;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageOutboxPublisher {
    private final OutboxStore store;
    private final EventPublisher transport;
    private final OutboxProperties settings;
    private final ScheduledTaskAuditService audit;
    private final MeterRegistry metrics;

    public void publishReady(UUID runId, Instant startedAt) {
        boolean started = false;
        for (int index = 0; index < settings.getBatchSize() && !Thread.currentThread().isInterrupted(); index++) {
            var candidate = store.claimNext();
            if (candidate.isEmpty()) break;
            if (!started) {
                audit.start(runId, startedAt);
                started = true;
            }
            var event = candidate.get();
            long clock = System.nanoTime();
            Exception failure = null;
            try {
                if (event.getAttempts() > settings.getMaxAttempts()) {
                    throw new IllegalStateException("Attempt limit reached after an expired claim");
                }
                transport.publish(event);
            } catch (Exception exception) {
                failure = exception;
                if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
                log.warn("Outbox publish failed runId={} eventId={} attempt={} error={}",
                        runId, event.getId(), event.getAttempts(), exception.getClass().getSimpleName());
            }
            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - clock);
            if (!store.settle(runId, event, failure, duration)) {
                log.warn("Outbox claim expired runId={} eventId={}", runId, event.getId());
            }
            String outcome = failure == null ? "success" : "failure";
            metrics.counter("messaging.outbox.publications", "outcome", outcome).increment();
            metrics.timer("messaging.outbox.publish.duration", "outcome", outcome).record(duration, TimeUnit.MILLISECONDS);
        }
    }
}
