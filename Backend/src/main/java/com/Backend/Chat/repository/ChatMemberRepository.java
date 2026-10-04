package com.Backend.Chat.repository;

import com.Backend.Chat.entity.Chat.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {

    List<ChatMember> findByUserId(Long userId);

    List<ChatMember> findByChatId(Long chatId);

    @Query("SELECT cm FROM ChatMember cm WHERE cm.chat.id = :chatId AND cm.user.id = :userId")
    ChatMember findByChatIdAndUserId(@Param("chatId") Long chatId, @Param("userId") Long userId);

    boolean existsByChatIdAndUserId(Long chatId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cm from ChatMember cm where cm.chat.id = :chatId and cm.user.id = :userId")
    ChatMember findForUpdate(@Param("chatId") Long chatId, @Param("userId") Long userId);

    @Query("SELECT COUNT(cm) > 0 FROM ChatMember cm WHERE cm.chat.id = :chatId AND cm.user.username = :username")
    boolean existsByChatIdAndUsername(@Param("chatId") Long chatId, @Param("username") String username);
}
