package com.Backend.Chat.service.implementation;

import com.Backend.Chat.Enums.MessageType;
import com.Backend.Chat.dto.Message.MessageDTO;
import com.Backend.Chat.dto.Message.SendMessageRequest;
import com.Backend.Chat.entity.Chat.Chat;
import com.Backend.Chat.entity.Message.Message;
import com.Backend.Chat.exception.ChatAccessDeniedException;
import com.Backend.Chat.repository.ChatMemberRepository;
import com.Backend.Chat.repository.ChatRepository;
import com.Backend.Chat.repository.MessageRepository;
import com.Backend.User.dto.UserDTO;
import com.Backend.User.entity.User;
import com.Backend.User.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageCommandService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final UserRepository userRepository;
    private final MessageOutboxWriter outboxWriter;

    public record SendResult(MessageDTO message, boolean created) {}

    @Transactional
    public SendResult send(Long chatId, SendMessageRequest request, String username) {
        User sender = userRepository.findByUsernameForUpdate(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (!chatMemberRepository.existsByChatIdAndUserId(chatId, sender.getId())) {
            throw new ChatAccessDeniedException(sender.getId(), chatId);
        }

        var previous = messageRepository.findBySender_IdAndClientMessageId(sender.getId(), request.clientMessageId());
        if (previous.isPresent()) {
            Message existing = previous.get();
            if (!existing.getChat().getId().equals(chatId) ||
                    (existing.getDeletedAt() == null && !existing.getMessageContent().equals(request.content()))) {
                throw new IllegalArgumentException("clientMessageId already used for another message");
            }
            return new SendResult(toDto(existing), false);
        }

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Chat not found: " + chatId));
        Message message = new Message();
        message.setChat(chat);
        message.setSender(sender);
        message.setMessageContent(request.content());
        message.setType(MessageType.TEXT);
        message.setClientMessageId(request.clientMessageId());
        Message saved = messageRepository.saveAndFlush(message);
        chatRepository.advanceLastActivity(chatId, saved.getSendedAt());
        outboxWriter.messageCreated(saved);
        return new SendResult(toDto(saved), true);
    }

    private MessageDTO toDto(Message message) {
        UserDTO sender = new UserDTO();
        sender.setId(message.getSender().getId());
        sender.setUsername(message.getSender().getUsername());
        return MessageDTO.builder()
                .id(message.getId())
                .chatId(message.getChat().getId())
                .content(message.getMessageContent())
                .type(message.getType())
                .sender(sender)
                .sendedAt(message.getSendedAt())
                .clientMessageId(message.getClientMessageId())
                .deletedAt(message.getDeletedAt())
                .build();
    }
}
