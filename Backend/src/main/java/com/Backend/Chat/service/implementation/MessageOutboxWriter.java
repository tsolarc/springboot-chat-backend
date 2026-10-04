package com.Backend.Chat.service.implementation;

import com.Backend.Chat.dto.Message.MessageCreatedEvent;
import com.Backend.Chat.dto.Message.ReadCursorEvent;
import com.Backend.Chat.entity.Message.Message;
import com.Backend.Chat.entity.Message.MessageOutboxEvent;
import com.Backend.Chat.repository.MessageOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.Backend.Common.Http.RequestIdFilter;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MessageOutboxWriter {

    private final MessageOutboxRepository repository;
    private final ObjectMapper objectMapper;

    public void messageCreated(Message message) {
        write(message, "message.created", message.getMessageContent());
    }

    public void messageUpdated(Message message) {
        write(message, "message.updated", message.getMessageContent());
    }

    public void messageDeleted(Message message) {
        write(message, "message.deleted", null);
    }

    public void readCursorAdvanced(Long chatId, Long userId, Long messageId) {
        UUID eventId = UUID.randomUUID();
        ReadCursorEvent payload = new ReadCursorEvent(eventId, 1, "conversation.read",
                chatId, userId, messageId, new java.util.Date());
        try {
            save(eventId, "conversation.read", objectMapper.writeValueAsString(payload),
                    userId, chatId, messageId, null, "READ_CURSOR", "ConversationSyncService.markRead");
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize read event", exception);
        }
    }

    private void write(Message message, String eventType, String content) {
        UUID eventId = UUID.randomUUID();
        MessageCreatedEvent payload = new MessageCreatedEvent(eventId, 1, eventType,
                message.getChat().getId(), message.getId(), message.getSender().getId(),
                message.getClientMessageId(), content, new java.util.Date());
        try {
            String operation = switch (eventType) {
                case "message.created" -> "MESSAGE_CREATE";
                case "message.updated" -> "MESSAGE_UPDATE";
                case "message.deleted" -> "MESSAGE_DELETE";
                default -> throw new IllegalArgumentException("Unsupported event type: " + eventType);
            };
            String source = "MESSAGE_CREATE".equals(operation)
                    ? "MessageCommandService.send" : "MessageServiceImpl." +
                    ("MESSAGE_UPDATE".equals(operation) ? "editMessage" : "deleteMessage");
            save(eventId, eventType, objectMapper.writeValueAsString(payload),
                    message.getSender().getId(), message.getChat().getId(), message.getId(),
                    message.getClientMessageId(), operation, source);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize message event", exception);
        }
    }

    private void save(UUID eventId, String routingKey, String payload,
                      Long userId, Long chatId, Long messageId, UUID clientMessageId,
                      String operation, String source) {
        MessageOutboxEvent event = new MessageOutboxEvent();
        event.setId(eventId);
        event.setRoutingKey(routingKey);
        event.setPayload(payload);
        event.setCreatedAt(Instant.now());
        event.setNextAttemptAt(event.getCreatedAt());
        event.setOriginUserId(userId);
        event.setOriginChatId(chatId);
        event.setOriginMessageId(messageId);
        event.setOriginClientMessageId(clientMessageId);
        event.setOriginOperation(operation);
        event.setOriginRequestId(MDC.get(RequestIdFilter.ATTRIBUTE));
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String route = attributes.getRequest().getMethod() + " " + attributes.getRequest().getRequestURI();
            event.setOriginSource(route.substring(0, Math.min(route.length(), 100)));
        } else {
            event.setOriginSource(source);
        }
        repository.save(event);
    }
}
