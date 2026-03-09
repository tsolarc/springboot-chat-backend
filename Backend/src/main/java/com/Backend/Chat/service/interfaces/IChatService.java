package com.Backend.Chat.service.interfaces;

import com.Backend.Chat.dto.Chat.ChatDTO;

import java.util.List;
import java.util.Optional;

public interface IChatService {

    ChatDTO createPrivateChat(Long user1Id, Long user2Id);
    Optional<ChatDTO> getChatByUsername(String username);
    ChatDTO getOrCreateDirectChat(Long userAId, Long userBId);
    Optional<ChatDTO> getChatById(Long chatId);
    List<ChatDTO> findAllByUser(Long userId);
    boolean userHasAccessToChat(String username, Long chatId);
    boolean isUserInChat(Long userId, Long chatId);
}
