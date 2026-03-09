package com.Backend.chat.interceptor;

import com.Backend.Chat.Interceptor.ChatChannelInterceptor;
import com.Backend.Chat.service.implementation.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ChatChannelInterceptorTest {

    @Mock
    private ChatServiceImpl chatService;

    @Mock
    private MessageChannel channel;

    @Mock
    private Principal principal;

    @InjectMocks
    private ChatChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        lenient().when(principal.getName()).thenReturn("testuser");
    }

    @Test
    void preSend_connectCommand_shouldPassThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setUser(principal);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        verifyNoInteractions(chatService);
    }

    @Test
    void preSend_subscribeWithAccess_shouldAllow() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chat.100.messages");
        accessor.setUser(principal);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(chatService.userHasAccessToChat("testuser", 100L)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        verify(chatService).userHasAccessToChat("testuser", 100L);
    }

    @Test
    void preSend_subscribeWithoutAccess_shouldThrow() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chat.100.messages");
        accessor.setUser(principal);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(chatService.userHasAccessToChat("testuser", 100L)).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("No tienes acceso a este chat");
    }

    @Test
    void preSend_subscribeWithoutUser_shouldThrow() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chat.100.messages");
        accessor.setUser(null);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("Usuario no autenticado");
    }

    @Test
    void preSend_sendWithAccess_shouldAllow() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/chat/100/send");
        accessor.setUser(principal);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(chatService.userHasAccessToChat("testuser", 100L)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        verify(chatService).userHasAccessToChat("testuser", 100L);
    }

    @Test
    void preSend_sendWithoutAccess_shouldThrow() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/chat/100/send");
        accessor.setUser(principal);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(chatService.userHasAccessToChat("testuser", 100L)).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("No tienes permiso para enviar mensajes");
    }

    @Test
    void preSend_sendWithoutUser_shouldThrow() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/chat/100/send");
        accessor.setUser(null);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("Usuario no autenticado");
    }

    @Test
    void preSend_nonChatDestination_shouldPassThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/other.destination");
        accessor.setUser(principal);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        verifyNoInteractions(chatService);
    }

    @Test
    void preSend_nullDestination_shouldPassThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(null);
        accessor.setUser(principal);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        verifyNoInteractions(chatService);
    }

    @Test
    void preSend_subscribeToTypingTopic_shouldValidateAccess() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chat.200.typing");
        accessor.setUser(principal);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(chatService.userHasAccessToChat("testuser", 200L)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        verify(chatService).userHasAccessToChat("testuser", 200L);
    }

    @Test
    void preSend_subscribeToReadTopic_shouldValidateAccess() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chat.300.read");
        accessor.setUser(principal);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(chatService.userHasAccessToChat("testuser", 300L)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        verify(chatService).userHasAccessToChat("testuser", 300L);
    }
}
