package com.Backend.Chat.dto.Message;

import java.util.Date;
import java.util.UUID;

public record MessageCreatedEvent(
        UUID eventId,
        int schemaVersion,
        String eventType,
        Long conversationId,
        Long messageId,
        Long senderId,
        UUID clientMessageId,
        String content,
        Date occurredAt
) {}
