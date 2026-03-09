package com.Backend.Chat.repository;

import com.Backend.Chat.entity.Message.MessageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageHistoryRepository extends JpaRepository<MessageHistory, Long> {
}
