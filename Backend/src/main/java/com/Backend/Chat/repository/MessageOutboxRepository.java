package com.Backend.Chat.repository;

import com.Backend.Chat.entity.Message.MessageOutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface MessageOutboxRepository extends JpaRepository<MessageOutboxEvent, UUID> {
    @Query(value = """
            SELECT * FROM message_outbox
            WHERE published_at IS NULL AND dead_lettered_at IS NULL AND next_attempt_at <= :now
              AND (claimed_until IS NULL OR claimed_until <= :now)
            ORDER BY created_at, id LIMIT 1 FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<MessageOutboxEvent> claimCandidate(@Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from MessageOutboxEvent e where e.id = :id and e.claimToken = :token")
    Optional<MessageOutboxEvent> findOwned(@Param("id") UUID id, @Param("token") UUID token);
}
