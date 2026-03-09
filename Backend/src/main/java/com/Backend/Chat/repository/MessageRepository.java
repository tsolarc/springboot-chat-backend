package com.Backend.Chat.repository;

import com.Backend.Chat.entity.Message.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m JOIN FETCH m.sender WHERE m.chat.id = :chatId ORDER BY m.sendedAt ASC")
    List<Message> findByChat_IdOrderBySendedAtAsc(@Param("chatId") Long chatId);

    Page<Message> findByChat_Id(Long chatId, Pageable pageable);

    @Query(value = "SELECT m FROM Message m JOIN FETCH m.sender WHERE m.chat.id = :chatId",
           countQuery = "SELECT COUNT(m) FROM Message m WHERE m.chat.id = :chatId")
    Page<Message> findByChat_IdWithSender(@Param("chatId") Long chatId, Pageable pageable);

    @Query("SELECT m.chat.id FROM Message m WHERE m.id = :msgId")
    Long findChatIdByMessageId(@Param("msgId") Long messageId);

    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId AND NOT EXISTS (SELECT 1 FROM m.readByUsers r WHERE KEY(r).id = :userId)")
    List<Message> findUnreadMessages(@Param("chatId") Long chatId, @Param("userId") Long userId);

    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId AND NOT EXISTS (SELECT 1 FROM m.readByUsers r WHERE KEY(r).id = :userId)")
    List<Message> findByChatIdAndNotReadBy(@Param("chatId") Long chatId, @Param("userId") Long userId);

    List<Message> findByChatId(Long chatId);

    Page<Message> findByChatIdOrderBySendedAtDesc(Long chatId, Pageable pageable);
}
