package com.Backend.Chat.exception;

public class MessageOwnershipException extends RuntimeException {

    public MessageOwnershipException(Long userId, Long messageId) {
        super("Usuario " + userId + " no es propietario del mensaje " + messageId);
    }

    public MessageOwnershipException(String message) {
        super(message);
    }
}
