package com.Backend.Chat.config;

/**
 * Constantes centralizadas para el módulo de Chat.
 */
public final class ChatConstants {

    private ChatConstants() {
        // Utility class
    }

    // RabbitMQ Routing Keys
    public static final String ROUTING_KEY_MESSAGE = "chat.message";
    public static final String ROUTING_KEY_EDIT = "chat.edit";
    public static final String ROUTING_KEY_DELETE = "chat.delete";

    // WebSocket Destinations
    public static final String TOPIC_CHAT_PREFIX = "/topic/chat.";
    public static final String TOPIC_TYPING_SUFFIX = ".typing";
    public static final String TOPIC_READ_SUFFIX = ".read";
    public static final String TOPIC_ERROR_SUFFIX = ".error";

    // Pagination defaults
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // Message constraints
    public static final int MAX_MESSAGE_LENGTH = 5000;
    public static final int MAX_USERNAME_LENGTH = 50;

    // Rate limiting
    public static final double DEFAULT_MESSAGES_PER_SECOND = 10.0;
    public static final int DEFAULT_MAX_CONNECTIONS_PER_USER = 5;
}
