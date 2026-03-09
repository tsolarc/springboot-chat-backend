package com.Backend.Chat.exception;

public class ChatNotFoundException extends RuntimeException {

    public ChatNotFoundException(Long chatId) {
        super("Chat no encontrado con ID: " + chatId);
    }

    public ChatNotFoundException(String message) {
        super(message);
    }
}
