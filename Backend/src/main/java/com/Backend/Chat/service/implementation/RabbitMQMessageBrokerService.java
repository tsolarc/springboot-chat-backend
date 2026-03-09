package com.Backend.Chat.service.implementation;

import com.Backend.Chat.dto.Message.MessageDTO;
import com.Backend.Chat.dto.ReadStatusDTO;
import com.Backend.Chat.dto.TypingDTO;
import com.Backend.Chat.service.interfaces.IMessageBrokerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class RabbitMQMessageBrokerService implements IMessageBrokerService {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitAdmin rabbitAdmin;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, List<Consumer<Object>>> subscribers = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final String exchange;

    public RabbitMQMessageBrokerService(RabbitTemplate rabbitTemplate, RabbitAdmin rabbitAdmin, SimpMessagingTemplate messagingTemplate, @Value("${rabbitmq.exchange:chat.exchange}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitAdmin = rabbitAdmin;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = new ObjectMapper();
        this.exchange = exchange;
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Override
    public void sendMessage(String destination, Object message) {
        rabbitTemplate.convertAndSend(exchange, destination, message);
    }

    @Override
    public void publishTopic(String destination, Object message) {
        messagingTemplate.convertAndSend(destination, message);
    }

    @Override
    public void publishError(Long chatId, String action, Long messageId, Exception exception) {
        String errorTopic = "/topic/chat." + chatId + ".error";
        Map<String, Object> payload = new HashMap<>();
        payload.put("error", "Failed to " + action);
        payload.put("chatId", chatId);
        payload.put("errorMessage", exception.getMessage());
        if (messageId != null) {
            payload.put("messageId", messageId);
        }
        messagingTemplate.convertAndSend(errorTopic, payload);
    }

    /**
     * Suscribe un handler a un destino específico.
     * Este método registra el consumidor y configura un RabbitListener para procesar los mensajes.
     *
     * @param destination La ruta de destino (por ejemplo, /topic/chat.123)
     * @param handler El consumidor que procesa los mensajes recibidos
     */
    @Override
    public void subscribe(String destination, Consumer<Object> handler) {
        log.info("Registrando suscripción para destino: {}", destination);

        String chatId = extractChatId(destination);
        if (chatId == null) {
            log.error("Formato de destino inválido: {}", destination);
            return;
        }

        // Registra el handler en la colección de suscriptores
        subscribers.computeIfAbsent(destination, k -> new CopyOnWriteArrayList<>()).add(handler);

        // Crea una cola específica para este destino si no existe
        String queueName = "queue.chat." + chatId;
        createQueueIfNotExists(queueName, "chat." + chatId);

        log.info("Suscripción registrada exitosamente para destino: {}", destination);
    }

    private String extractChatId(String destination) {
        if (destination == null) return null;

        Pattern pattern = Pattern.compile("/topic/chat\\.(\\d+)");
        Matcher matcher = pattern.matcher(destination);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Método para crear una cola si no existe y vincularla al exchange
     */
    private void createQueueIfNotExists(String queueName, String routingKey) {
        try {
            // Declaración de la cola
            rabbitAdmin.declareQueue(new Queue(queueName, true));

            // Vinculación al exchange
            rabbitAdmin.declareBinding(
                    new Binding(queueName, Binding.DestinationType.QUEUE, exchange, routingKey, null));

            log.info("Cola {} creada y vinculada al exchange {} con routing key {}",
                    queueName, exchange, routingKey);
        } catch (Exception exception) {
            log.error("Error al crear la cola {}: {}", queueName, exception.getMessage(), exception);
        }
    }

    /**
     * Método que maneja mensajes entrantes desde RabbitMQ
     * Este método sería anotado con @RabbitListener en una implementación real
     */
    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void handleMessage(Message message, @Header("routing-key") String routingKey) {
        try {
            String chatId = routingKey.replace("chat.", "");
            String destination = "/topic/chat." + chatId;
            Object payload = deserializeMessage(message);

            if (subscribers.containsKey(destination)) {
                subscribers.get(destination).forEach(handler -> {
                    try {
                        handler.accept(payload);
                    } catch (Exception exception) {
                        log.error("Error al procesar mensaje para {}: {}", destination, exception.getMessage(), exception);
                    }
                });
            }
        } catch (Exception exception) {
            log.error("Error al manejar mensaje de RabbitMQ: {}", exception.getMessage(), exception);
        }
    }

    /**
     * Deserializa el mensaje según su tipo
     */
    private Object deserializeMessage(Message amqpMessage) {
        try {
            String contentType = amqpMessage.getMessageProperties().getContentType();
            byte[] body = amqpMessage.getBody();

            if (contentType != null) {
                if (contentType.contains("chat.message")) {
                    return objectMapper.readValue(body, MessageDTO.class);
                } else if (contentType.contains("chat.typing")) {
                    return objectMapper.readValue(body, TypingDTO.class);
                } else if (contentType.contains("chat.read")) {
                    return objectMapper.readValue(body, ReadStatusDTO.class);
                }
            }
            return objectMapper.readValue(body, Object.class);
        } catch (Exception exception) {
            log.error("Error deserializing message: {}", exception.getMessage());
            return null;
        }
    }
}
