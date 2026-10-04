package com.Backend.Chat.dto.Chat;

import java.util.List;

public record ConversationPage(List<ConversationSummary> conversations, String nextCursor) {}
