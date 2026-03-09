package com.Backend.chat.interceptor;

import com.Backend.Chat.Interceptor.RateLimitingChannelInterceptor;
import com.Backend.Chat.Limiter.ConcurrentSessionConnectionLimiter;
import com.Backend.Chat.Limiter.PerUserRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class RateLimitingChannelInterceptorTest {

    @Mock
    private ConcurrentSessionConnectionLimiter connectionLimiter;

    @Mock
    private PerUserRateLimiter perUserRateLimiter;

    @Mock
    private MessageChannel channel;

    @Mock
    private Principal principal;

    private RateLimitingChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitingChannelInterceptor(connectionLimiter, perUserRateLimiter);
        lenient().when(principal.getName()).thenReturn("testuser");
    }

    @Test
    void preSend_connectWithinLimit_shouldAllow() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setUser(principal);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(connectionLimiter.allowConnection("testuser")).thenReturn(true);

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        verify(connectionLimiter).allowConnection("testuser");
    }

    @Test
    void preSend_connectExceedingLimit_shouldThrow() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setUser(principal);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(connectionLimiter.allowConnection("testuser")).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("Too many concurrent connections");
    }

    @Test
    void preSend_connectWithoutUser_shouldPassThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setUser(null);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        verifyNoInteractions(connectionLimiter);
    }

    @Test
    void preSend_sendWithinRateLimit_shouldAllow() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setUser(principal);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(perUserRateLimiter.tryAcquire("testuser")).thenReturn(true);

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        verify(perUserRateLimiter).tryAcquire("testuser");
    }

    @Test
    void preSend_sendExceedingRateLimit_shouldThrow() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setUser(principal);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(perUserRateLimiter.tryAcquire("testuser")).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("Rate limit exceeded");
    }

    @Test
    void preSend_sendWithoutUser_shouldPassThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setUser(null);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        verifyNoInteractions(perUserRateLimiter);
    }

    @Test
    void preSend_disconnect_shouldCleanupResources() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setUser(principal);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        verify(connectionLimiter).decrementConnectionCount("testuser");
        verify(perUserRateLimiter).removeUser("testuser");
    }

    @Test
    void preSend_disconnectWithoutUser_shouldNotCleanup() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setUser(null);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        verifyNoInteractions(connectionLimiter);
        verifyNoInteractions(perUserRateLimiter);
    }

    @Test
    void preSend_subscribeCommand_shouldPassThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(principal);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        verifyNoInteractions(connectionLimiter);
        verifyNoInteractions(perUserRateLimiter);
    }

    @Test
    void rateLimiting_perUser_shouldIsolateUsers() {
        // Test that user1's rate limit doesn't affect user2
        Principal user1 = mock(Principal.class);
        Principal user2 = mock(Principal.class);
        when(user1.getName()).thenReturn("user1");
        when(user2.getName()).thenReturn("user2");

        StompHeaderAccessor accessor1 = StompHeaderAccessor.create(StompCommand.SEND);
        accessor1.setUser(user1);
        Message<?> message1 = MessageBuilder.createMessage(new byte[0], accessor1.getMessageHeaders());

        StompHeaderAccessor accessor2 = StompHeaderAccessor.create(StompCommand.SEND);
        accessor2.setUser(user2);
        Message<?> message2 = MessageBuilder.createMessage(new byte[0], accessor2.getMessageHeaders());

        when(perUserRateLimiter.tryAcquire("user1")).thenReturn(false);
        when(perUserRateLimiter.tryAcquire("user2")).thenReturn(true);

        // user1 should be rate limited
        assertThatThrownBy(() -> interceptor.preSend(message1, channel))
                .isInstanceOf(MessageDeliveryException.class);

        // user2 should not be affected
        Message<?> result = interceptor.preSend(message2, channel);
        assertThat(result).isNotNull();
    }
}
