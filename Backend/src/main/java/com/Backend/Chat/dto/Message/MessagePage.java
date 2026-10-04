package com.Backend.Chat.dto.Message;

import java.util.List;

public record MessagePage(List<MessageDTO> messages, String nextCursor) {}
