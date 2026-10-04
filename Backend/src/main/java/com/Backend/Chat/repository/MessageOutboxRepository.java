package com.Backend.Chat.repository;

import com.Backend.Chat.entity.Message.MessageOutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessageOutboxRepository extends JpaRepository<MessageOutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from MessageOutboxEvent e where e.publishedAt is null and e.nextAttemptAt <= :now order by e.createdAt, e.id")
    List<MessageOutboxEvent> findReadyForUpdate(@Param("now") Instant now, Pageable pageable);
}
