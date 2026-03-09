package com.Backend.Chat.service.interfaces;

import java.util.function.Consumer;

public interface IMessageBrokerService {
    void sendMessage(String destination, Object message);
    void publishTopic(String destination, Object message);
    void publishError(Long chatId, String action, Long messageId, Exception exception);
    void subscribe(String destination, Consumer<Object> handler);
}
