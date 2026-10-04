package com.Backend.Chat.dto.Message;

public record ReadCursorResponse(Long conversationId, Long userId, Long messageId) {}
