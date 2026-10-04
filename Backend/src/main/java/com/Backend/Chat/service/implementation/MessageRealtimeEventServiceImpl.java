package com.Backend.Chat.service.implementation;

import com.Backend.Chat.config.ChatConstants;
import com.Backend.Chat.dto.ReadStatusDTO;
import com.Backend.Chat.dto.TypingDTO;
import com.Backend.Chat.service.interfaces.IMessageBrokerService;
import com.Backend.Chat.service.interfaces.IMessageRealtimeEventService;
import com.Backend.Chat.service.interfaces.IMessageService;
import com.Backend.Chat.service.interfaces.IChatService;
import com.Backend.Chat.exception.ChatAccessDeniedException;
import com.Backend.User.dto.UserDTO;
import com.Backend.User.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageRealtimeEventServiceImpl implements IMessageRealtimeEventService {

    private final IUserService userService;
    private final ConversationSyncService conversationSyncService;
    private final IChatService chatService;
    private final IMessageBrokerService messageBrokerService;

    @Override
    public void initializeReadStatusEvent(Long chatId, Long messageId, Principal principal) {
        Optional<UserDTO> maybeUser = resolveAuthenticatedUser(principal,
                "mark message as read", chatId, messageId);
        if (maybeUser.isEmpty()) {
            return;
        }
        processReadEvent(chatId, messageId, maybeUser.get().getUsername());
    }

    @Override
    public void initializeTypingEvent(Long chatId, Map<String, Object> payload, Principal principal) {
        Optional<UserDTO> maybeUser = resolveAuthenticatedUser(principal,
                "send typing notification", chatId, null);
        if (maybeUser.isEmpty()) {
            return;
        }
        boolean isTyping = extractTypingStatus(payload);
        processTypingEvent(chatId, maybeUser.get(), isTyping);
    }

    private Optional<UserDTO> resolveAuthenticatedUser(Principal principal, String action, Long chatId, Long messageId) {
        if (principal == null) {
            log.warn("Unauthenticated attempt to {}. chatId: {}, messageId: {}", action, chatId, messageId);
            return Optional.empty();
        }

        try {
            return Optional.of(userService.findByUsername(principal.getName()));
        } catch (Exception exception) {
            log.error("Failed to resolve authenticated user for {}. chatId: {}, messageId: {}", action, chatId, messageId, exception);
            return Optional.empty();
        }
    }

    private void processReadEvent(Long chatId, Long messageId, String username) {
        try {
            conversationSyncService.markRead(chatId, messageId, username);
        } catch (Exception exception) {
            log.error("Failed to mark message as read. chatId: {}, messageId: {}", chatId, messageId, exception);
        }
    }

    private void processTypingEvent(Long chatId, UserDTO user, boolean isTyping) {
        try {
            if (!chatService.isUserInChat(user.getId(), chatId)) {
                throw new ChatAccessDeniedException(user.getId(), chatId);
            }
            TypingDTO typingDTO = new TypingDTO(user.getId(), user.getUsername(), chatId, new Date(), isTyping);
            String typingTopic = ChatConstants.TOPIC_CHAT_PREFIX + chatId + ChatConstants.TOPIC_TYPING_SUFFIX;
            messageBrokerService.publishTopic(typingTopic, typingDTO);
        } catch (Exception exception) {
            log.error("Failed to send typing event. chatId: {}", chatId, exception);
        }
    }

    private boolean extractTypingStatus(Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("isTyping")) {
            return true;
        }
        return Boolean.parseBoolean(String.valueOf(payload.get("isTyping")));
    }
}
