package com.Backend.Chat.outbox;

import com.Backend.Chat.entity.Message.MessageOutboxEvent;
import com.Backend.Chat.repository.MessageOutboxRepository;
import com.Backend.Chat.service.implementation.ScheduledTaskAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Short transactions around claims and settlement. No network calls while holding row locks. */
@Service
@RequiredArgsConstructor
public class OutboxStore {
    private final MessageOutboxRepository repository;
    private final OutboxProperties settings;
    private final ScheduledTaskAuditService audit;

    @Transactional
    public Optional<MessageOutboxEvent> claimNext() {
        Instant now = Instant.now();
        return repository.claimCandidate(now).map(event -> {
            event.setClaimToken(UUID.randomUUID());
            event.setClaimedUntil(now.plus(settings.getLeaseDuration()));
            event.setAttempts(event.getAttempts() + 1);
            return event;
        });
    }

    @Transactional
    public boolean settle(UUID runId, MessageOutboxEvent claim, Exception failure, long durationMs) {
        var owned = repository.findOwned(claim.getId(), claim.getClaimToken());
        if (owned.isEmpty()) return false; // A newer claim owns the row; this worker must not overwrite it.
        MessageOutboxEvent event = owned.get();
        Instant now = Instant.now();
        if (failure == null) {
            event.setPublishedAt(now);
        } else if (event.getAttempts() >= settings.getMaxAttempts()) {
            event.setDeadLetteredAt(now);
        } else {
            long capMs = Math.min(300_000L, 1000L << Math.min(event.getAttempts(), 8));
            event.setNextAttemptAt(now.plusMillis(ThreadLocalRandom.current().nextLong(capMs / 2, capMs + 1)));
        }
        event.setClaimToken(null);
        event.setClaimedUntil(null);
        audit.recordAttempt(runId, event, failure, durationMs);
        return true;
    }
}
