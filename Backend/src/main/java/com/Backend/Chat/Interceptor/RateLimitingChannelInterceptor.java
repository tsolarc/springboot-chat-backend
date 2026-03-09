package com.Backend.Chat.Interceptor;

import com.Backend.Chat.Limiter.ConcurrentSessionConnectionLimiter;
import com.Backend.Chat.Limiter.PerUserRateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class RateLimitingChannelInterceptor implements ChannelInterceptor {

    private final ConcurrentSessionConnectionLimiter connectionLimiter;
    private final PerUserRateLimiter perUserRateLimiter;

    public RateLimitingChannelInterceptor(@Qualifier("sessionConnectionLimiter") ConcurrentSessionConnectionLimiter connectionLimiter, PerUserRateLimiter perUserRateLimiter) {
        this.connectionLimiter = connectionLimiter;
        this.perUserRateLimiter = perUserRateLimiter;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor.getCommand() == StompCommand.CONNECT) {
            Principal user = accessor.getUser();
            if (user != null) {
                if (!connectionLimiter.allowConnection(user.getName())) {
                    throw new MessageDeliveryException("Too many concurrent connections");
                }
            }
        } else if (accessor.getCommand() == StompCommand.SEND) {
            Principal user = accessor.getUser();
            if (user != null && !perUserRateLimiter.tryAcquire(user.getName())) {
                throw new MessageDeliveryException("Rate limit exceeded");
            }
        } else if (accessor.getCommand() == StompCommand.DISCONNECT) {
            Principal user = accessor.getUser();
            if (user != null) {
                connectionLimiter.decrementConnectionCount(user.getName());
                perUserRateLimiter.removeUser(user.getName());
            }
        }
        return message;
    }
}
