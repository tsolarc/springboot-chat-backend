package com.Backend.Chat.outbox;

import com.Backend.Chat.entity.Message.MessageOutboxEvent;

/** Transport port. Returns only after durable broker acceptance; delivery is at least once. */
public interface EventPublisher {
    void publish(MessageOutboxEvent event) throws Exception;
}
