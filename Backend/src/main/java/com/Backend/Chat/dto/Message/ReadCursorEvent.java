package com.Backend.Chat.dto.Message;

import java.util.Date;
import java.util.UUID;

public record ReadCursorEvent(UUID eventId, int schemaVersion, String eventType,
                              Long conversationId, Long userId, Long messageId, Date occurredAt) {}
