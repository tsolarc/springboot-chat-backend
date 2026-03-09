package com.Backend.chat.RabbitMQ;

import com.Backend.Chat.dto.Message.MessageDTO;
import com.Backend.Chat.service.implementation.RabbitMQMessageBrokerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RabbitMQMessageBrokerServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RabbitAdmin rabbitAdmin;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private RabbitMQMessageBrokerService messageBrokerService;

    private final String exchangeName = "chat.exchange";

    @BeforeEach
    void setUp() {
        messageBrokerService = new RabbitMQMessageBrokerService(rabbitTemplate, rabbitAdmin, messagingTemplate, exchangeName);
    }

    @Test
    void sendMessage_sendsMessageToCorrectDestination() {
        String destination = "chat.123";
        MessageDTO message = MessageDTO.builder().id(1L).content("Hola Mundo").build();

        messageBrokerService.sendMessage(destination, message);

        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);

        verify(rabbitTemplate).convertAndSend(
                exchangeCaptor.capture(),
                routingKeyCaptor.capture(),
                messageCaptor.capture()
        );

        assertEquals(exchangeName, exchangeCaptor.getValue());
        assertEquals(destination, routingKeyCaptor.getValue());
        assertEquals(message, messageCaptor.getValue());
    }

    @Test
    void subscribe_withValidDestination_createsQueueAndBinding() {
        String destination = "/topic/chat.123";
        String expectedQueueName = "queue.chat.123";
        String expectedRoutingKey = "chat.123";

        messageBrokerService.subscribe(destination, (msg) -> {});

        ArgumentCaptor<org.springframework.amqp.core.Queue> queueCaptor = ArgumentCaptor.forClass(org.springframework.amqp.core.Queue.class);
        verify(rabbitAdmin).declareQueue(queueCaptor.capture());
        assertEquals(expectedQueueName, queueCaptor.getValue().getName());

        ArgumentCaptor<org.springframework.amqp.core.Binding> bindingCaptor = ArgumentCaptor.forClass(org.springframework.amqp.core.Binding.class);
        verify(rabbitAdmin).declareBinding(bindingCaptor.capture());
        assertEquals(expectedQueueName, bindingCaptor.getValue().getDestination());
        assertEquals(expectedRoutingKey, bindingCaptor.getValue().getRoutingKey());
        assertEquals(exchangeName, bindingCaptor.getValue().getExchange());
    }

    @Test
    void subscribe_withInvalidDestination_doesNotCreateQueue() {
        String invalidDestination = "/topic/invalid-format";

        messageBrokerService.subscribe(invalidDestination, (msg) -> {});

        verify(rabbitAdmin, never()).declareQueue(any());
        verify(rabbitAdmin, never()).declareBinding(any());
    }

    @Test
    void sendMessage_whenRabbitThrowsException_propagatesException() {
        String destination = "chat.123";
        MessageDTO message = MessageDTO.builder().id(1L).build();

        doThrow(new AmqpException("Connection failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(MessageDTO.class));

        assertThrows(AmqpException.class, () ->
                messageBrokerService.sendMessage(destination, message)
        );
    }
}
