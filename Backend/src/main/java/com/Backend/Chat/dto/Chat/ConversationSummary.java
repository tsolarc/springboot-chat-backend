package com.Backend.Chat.dto.Chat;

import java.util.Date;

public record ConversationSummary(Long id, String name, Date lastActivityAt, Long lastReadMessageId) {}
