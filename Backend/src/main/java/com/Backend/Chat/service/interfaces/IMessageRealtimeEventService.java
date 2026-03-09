package com.Backend.Chat.service.interfaces;

import java.security.Principal;
import java.util.Map;

public interface IMessageRealtimeEventService {
    void initializeReadStatusEvent(Long chatId, Long messageId, Principal principal);
    void initializeTypingEvent(Long chatId, Map<String, Object> payload, Principal principal);
}
