package com.Backend.Chat.service.implementation;

import com.Backend.Chat.entity.Message.MessageOutboxEvent;
import com.Backend.Chat.repository.MessageOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageOutboxPublisher {

    public record EventAttempt(UUID eventId, String routingKey, Long userId, Long chatId,
                               Long messageId, UUID clientMessageId, String operation, String source, int attemptNumber,
                               boolean success, long durationMs, String errorType, String errorMessage) {}

    public record PublishResult(List<EventAttempt> attempts) {
        public long successes() { return attempts.stream().filter(EventAttempt::success).count(); }
        public long failures() { return attempts.size() - successes(); }
    }

    private final MessageOutboxRepository repository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public PublishResult publishReady() {
        List<EventAttempt> attempts = new ArrayList<>();
        for (MessageOutboxEvent event : repository.findReadyForUpdate(Instant.now(), PageRequest.of(0, 50))) {
            long started = System.nanoTime();
            int attemptNumber = event.getAttempts() + 1;
            String errorType = null;
            String errorMessage = null;
            boolean success = false;
            try {
                MessageProperties properties = new MessageProperties();
                properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                properties.setDeliveryMode(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
                properties.setMessageId(event.getId().toString());
                Message message = new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), properties);
                CorrelationData correlation = new CorrelationData(event.getId().toString());
                rabbitTemplate.send("messaging.events", event.getRoutingKey(), message, correlation);
                CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
                if (!confirm.isAck() || correlation.getReturned() != null) {
                    throw new IllegalStateException("Broker did not route event " + event.getId());
                }
                event.setPublishedAt(Instant.now());
                success = true;
            } catch (Exception exception) {
                errorType = exception.getClass().getName();
                errorMessage = exception.getMessage();
                event.setAttempts(event.getAttempts() + 1);
                long delay = Math.min(300, 1L << Math.min(event.getAttempts(), 8));
                event.setNextAttemptAt(Instant.now().plusSeconds(delay));
                log.warn("Will retry outbox event {} after publish failure: {}", event.getId(), exception.toString());
            } finally {
                attempts.add(new EventAttempt(event.getId(), event.getRoutingKey(), event.getOriginUserId(),
                        event.getOriginChatId(), event.getOriginMessageId(), event.getOriginClientMessageId(),
                        event.getOriginOperation(),
                        event.getOriginSource(), attemptNumber, success,
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started), errorType, errorMessage));
            }
        }
        return new PublishResult(List.copyOf(attempts));
    }
}
