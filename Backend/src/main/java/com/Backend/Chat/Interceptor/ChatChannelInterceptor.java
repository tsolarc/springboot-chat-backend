package com.Backend.Chat.Interceptor;

import com.Backend.Chat.service.implementation.ChatServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatChannelInterceptor implements ChannelInterceptor {

    private static final Pattern CHAT_DESTINATION_PATTERN = Pattern.compile("/topic/chat\\.(\\d+).*");
    private static final Pattern APP_CHAT_PATTERN = Pattern.compile("/app/chat/(\\d+)/.*");

    private final ChatServiceImpl chatService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            log.debug("WebSocket CONNECT recibido");
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            validateChatAccess(accessor, "SUBSCRIBE");
        } else if (StompCommand.SEND.equals(command)) {
            validateSendAccess(accessor);
        }

        return message;
    }

    private void validateChatAccess(StompHeaderAccessor accessor, String operation) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Matcher matcher = CHAT_DESTINATION_PATTERN.matcher(destination);
        if (matcher.matches()) {
            Long chatId = Long.parseLong(matcher.group(1));
            Principal user = accessor.getUser();

            if (user == null) {
                log.warn("{} rechazado: usuario no autenticado para destino {}", operation, destination);
                throw new MessageDeliveryException("Usuario no autenticado");
            }

            String username = user.getName();
            if (!chatService.userHasAccessToChat(username, chatId)) {
                log.warn("{} rechazado: usuario {} sin acceso al chat {}", operation, username, chatId);
                throw new MessageDeliveryException("No tienes acceso a este chat");
            }

            log.debug("{} autorizado: usuario {} para chat {}", operation, username, chatId);
        }
    }

    private void validateSendAccess(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Matcher matcher = APP_CHAT_PATTERN.matcher(destination);
        if (matcher.matches()) {
            Long chatId = Long.parseLong(matcher.group(1));
            Principal user = accessor.getUser();

            if (user == null) {
                log.warn("SEND rechazado: usuario no autenticado para destino {}", destination);
                throw new MessageDeliveryException("Usuario no autenticado");
            }

            String username = user.getName();
            if (!chatService.userHasAccessToChat(username, chatId)) {
                log.warn("SEND rechazado: usuario {} sin acceso al chat {}", username, chatId);
                throw new MessageDeliveryException("No tienes permiso para enviar mensajes a este chat");
            }

            log.debug("SEND autorizado: usuario {} para chat {}", username, chatId);
        }
    }
}
