package com.Backend.Chat.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.Backend.Chat.entity.Chat.Chat;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query("""
      SELECT chat
      FROM Chat AS chat
      JOIN chat.members user1
      JOIN chat.members user2
      WHERE user1.id = :user1Id AND user2.id = :user2Id
      AND chat.chatType = com.Backend.Chat.Enums.ChatType.PRIVATE """)
    Optional<Chat> findDirectChat(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    @EntityGraph(attributePaths = {"members", "members.user"})
    List<Chat> findAllByMembers_User_Id(Long userId);

    @EntityGraph(attributePaths = {"members", "members.user"})
    Optional<Chat> findById(Long id);

    @Query("SELECT c FROM Chat c JOIN c.members m JOIN m.user u WHERE u.username = :username")
    Optional<Chat> getChatByUsername(@Param("username") String username);

    @Query("SELECT c FROM Chat c JOIN c.members m JOIN m.user u WHERE u.username = :username")
    List<Chat> findChatsByParticipantUsername(@Param("username") String username);

    @Query("SELECT c FROM Chat c JOIN c.members m JOIN m.user u WHERE u.username = :username AND c.chatName LIKE %:conversationName%")
    Optional<Chat> findChatByUsernameAndConversationName(@Param("username") String username, @Param("conversationName") String conversationName);

    @Query("SELECT c FROM Chat c JOIN c.members m WHERE c.isPrivate = true AND m.user.id = :userId")
    List<Chat> findPrivateChatsForUser(@Param("userId") Long userId);

    @Query("SELECT c FROM Chat c WHERE c.isPrivate = true AND EXISTS " +
            "(SELECT m1 FROM ChatMember m1 WHERE m1.chat = c AND m1.user.id = :user1Id) AND EXISTS " +
            "(SELECT m2 FROM ChatMember m2 WHERE m2.chat = c AND m2.user.id = :user2Id)")
    Optional<Chat> findPrivateChatBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}
