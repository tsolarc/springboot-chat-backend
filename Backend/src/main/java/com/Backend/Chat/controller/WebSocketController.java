package com.Backend.Chat.controller;

import com.Backend.Chat.dto.Message.SendMessageRequest;
import com.Backend.Chat.service.implementation.MessageCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import jakarta.validation.Valid;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final MessageCommandService messageCommandService;

    @MessageMapping("/chat/{chatId}/send")
    public void sendMessage(@DestinationVariable Long chatId, @Valid @Payload SendMessageRequest request, Principal principal) {
        MessageCommandService.SendResult result = messageCommandService.send(chatId, request, principal.getName());
        log.debug("Message {} persisted for chat {} (created={})", result.message().getId(), chatId, result.created());
    }
}
