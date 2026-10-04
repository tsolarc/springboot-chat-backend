package com.Backend.Chat.service.implementation;

import com.Backend.Chat.config.ChatConstants;
import com.Backend.Chat.outbox.RealtimeBrokerHealth;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RealtimeEventConsumer {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final RealtimeBrokerHealth broker;

    @RabbitListener(queues = "chat.realtime.queue")
    public void forward(Message message) throws IOException {
        if (!broker.isAvailable()) {
            throw new IllegalStateException("STOMP relay unavailable");
        }
        JsonNode event = objectMapper.readTree(message.getBody());
        if (event == null || !event.path("eventId").isTextual() ||
                !event.path("conversationId").canConvertToLong() || event.path("conversationId").asLong() <= 0 ||
                !java.util.Set.of("message.created", "message.updated", "message.deleted", "conversation.read")
                        .contains(event.path("eventType").asText())) {
            throw new AmqpRejectAndDontRequeueException("Invalid realtime event envelope");
        }
        messagingTemplate.convertAndSend(ChatConstants.TOPIC_CHAT_PREFIX + event.get("conversationId").asLong(), event);
    }
}
