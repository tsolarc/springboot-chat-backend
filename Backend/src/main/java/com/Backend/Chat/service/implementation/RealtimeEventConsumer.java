package com.Backend.Chat.service.implementation;

import com.Backend.Chat.config.ChatConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class RealtimeEventConsumer implements ApplicationListener<BrokerAvailabilityEvent> {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final AtomicBoolean brokerAvailable = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(BrokerAvailabilityEvent event) {
        brokerAvailable.set(event.isBrokerAvailable());
    }

    @RabbitListener(queues = "chat.realtime.queue")
    public void forward(Message message) throws IOException {
        if (!brokerAvailable.get()) {
            throw new IllegalStateException("STOMP relay unavailable");
        }
        JsonNode event = objectMapper.readTree(message.getBody());
        if (!event.hasNonNull("eventId") || !event.hasNonNull("conversationId") || !event.hasNonNull("eventType")) {
            throw new IllegalArgumentException("Invalid realtime event");
        }
        messagingTemplate.convertAndSend(ChatConstants.TOPIC_CHAT_PREFIX + event.get("conversationId").asLong(), event);
    }
}
