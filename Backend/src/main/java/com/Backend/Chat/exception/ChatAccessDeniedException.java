package com.Backend.Chat.exception;

public class ChatAccessDeniedException extends RuntimeException {

    public ChatAccessDeniedException(Long userId, Long chatId) {
        super("Usuario " + userId + " no tiene acceso al chat " + chatId);
    }

    public ChatAccessDeniedException(String message) {
        super(message);
    }
}
