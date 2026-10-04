package com.Backend.Chat.service.implementation;

import com.Backend.Chat.dto.Chat.ConversationPage;
import com.Backend.Chat.dto.Chat.ConversationSummary;
import com.Backend.Chat.dto.Message.MessagePage;
import com.Backend.Chat.dto.Message.ReadCursorResponse;
import com.Backend.Chat.entity.Chat.Chat;
import com.Backend.Chat.entity.Chat.ChatMember;
import com.Backend.Chat.entity.Message.Message;
import com.Backend.Chat.exception.ChatAccessDeniedException;
import com.Backend.Chat.mapper.MessageMapper;
import com.Backend.Chat.repository.ChatMemberRepository;
import com.Backend.Chat.repository.ChatRepository;
import com.Backend.Chat.repository.MessageRepository;
import com.Backend.User.entity.User;
import com.Backend.User.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationSyncService {

    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final MessageOutboxWriter outboxWriter;

    @Transactional(readOnly = true)
    public MessagePage messages(Long chatId, String before, int limit, String username) {
        User user = user(username);
        requireMember(chatId, user.getId());
        int pageSize = validateLimit(limit);
        List<Message> rows;
        if (before == null || before.isBlank()) {
            rows = messageRepository.findByChat_IdOrderBySendedAtDescIdDesc(chatId, PageRequest.of(0, pageSize + 1));
        } else {
            SeekCursor cursor = SeekCursor.decode(before);
            rows = messageRepository.findBefore(chatId, cursor.at(), cursor.id(), PageRequest.of(0, pageSize + 1));
        }
        boolean hasMore = rows.size() > pageSize;
        List<Message> page = rows.subList(0, Math.min(rows.size(), pageSize));
        String next = hasMore ? SeekCursor.encode(page.get(page.size() - 1).getSendedAt(), page.get(page.size() - 1).getId()) : null;
        return new MessagePage(page.stream().map(messageMapper::convertToDTO).toList(), next);
    }

    @Transactional(readOnly = true)
    public ConversationPage conversations(String before, int limit, String username) {
        User user = user(username);
        int pageSize = validateLimit(limit);
        SeekCursor cursor = before == null || before.isBlank() ? null : SeekCursor.decode(before);
        List<Chat> rows = chatRepository.findConversationPage(user.getId(), cursor == null ? null : cursor.at(),
                cursor == null ? null : cursor.id(), PageRequest.of(0, pageSize + 1));
        boolean hasMore = rows.size() > pageSize;
        List<Chat> page = rows.subList(0, Math.min(rows.size(), pageSize));
        List<ConversationSummary> summaries = page.stream().map(chat -> {
            ChatMember member = chatMemberRepository.findByChatIdAndUserId(chat.getId(), user.getId());
            return new ConversationSummary(chat.getId(), chat.getChatName(), chat.getLastActivityAt(),
                    member == null ? null : member.getLastReadMessageId());
        }).toList();
        String next = hasMore ? SeekCursor.encode(page.get(page.size() - 1).getLastActivityAt(), page.get(page.size() - 1).getId()) : null;
        return new ConversationPage(summaries, next);
    }

    @Transactional
    public ReadCursorResponse markRead(Long chatId, Long messageId, String username) {
        User user = user(username);
        ChatMember member = chatMemberRepository.findForUpdate(chatId, user.getId());
        if (member == null) {
            throw new ChatAccessDeniedException(user.getId(), chatId);
        }
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));
        if (!message.getChat().getId().equals(chatId)) {
            throw new ChatAccessDeniedException(user.getId(), chatId);
        }
        Long previous = member.getLastReadMessageId();
        if (previous == null || messageId > previous) {
            member.setLastReadMessageId(messageId);
            outboxWriter.readCursorAdvanced(chatId, user.getId(), messageId);
        }
        return new ReadCursorResponse(chatId, user.getId(), member.getLastReadMessageId());
    }

    private User user(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private void requireMember(Long chatId, Long userId) {
        if (!chatMemberRepository.existsByChatIdAndUserId(chatId, userId)) {
            throw new ChatAccessDeniedException(userId, chatId);
        }
    }

    private int validateLimit(int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100");
        return limit;
    }
}
