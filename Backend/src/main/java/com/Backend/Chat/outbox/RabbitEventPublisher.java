package com.Backend.Chat.outbox;

import com.Backend.Chat.entity.Message.MessageOutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbit;
    private final OutboxProperties settings;

    @Override
    public void publish(MessageOutboxEvent event) throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(event.getId().toString());

        if (event.getOriginRequestId() != null) properties.setHeader("requestId", event.getOriginRequestId());
        CorrelationData correlation = new CorrelationData(event.getId().toString());

        rabbit.send("messaging.events", event.getRoutingKey(),
                new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), properties), correlation);

        var confirm = correlation.getFuture().get(settings.getConfirmTimeout().toMillis(), TimeUnit.MILLISECONDS);

        if (!confirm.isAck() || correlation.getReturned() != null) {
            throw new IllegalStateException("Broker rejected or could not route event " + event.getId());
        }
    }
}
