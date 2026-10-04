package com.Backend.Chat.Interceptor;

import com.Backend.Security.Service.JWTService;
import com.Backend.Security.Service.SpringSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthenticationInterceptor implements ChannelInterceptor {

    private final JWTService jwtService;
    private final SpringSecurityService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new MessageDeliveryException("Token requerido");
        }
        String token = authorization.substring(7);
        if (!jwtService.isTokenValid(token)) {
            throw new MessageDeliveryException("Token inválido");
        }
        try {
            UserDetails user = userDetailsService.loadUserByUsername(jwtService.extractUsername(token));
            accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        } catch (RuntimeException exception) {
            throw new MessageDeliveryException(message, "Usuario inválido", exception);
        }
        return message;
    }
}
