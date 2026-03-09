package com.Backend.Chat.exception;

public class MessageNotFoundException extends RuntimeException {

    public MessageNotFoundException(Long messageId) {
        super("Mensaje no encontrado con ID: " + messageId);
    }

    public MessageNotFoundException(String message) {
        super(message);
    }
}
