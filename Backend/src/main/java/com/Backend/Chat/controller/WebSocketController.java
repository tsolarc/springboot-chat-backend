package com.Backend.Chat.controller;

import com.Backend.Chat.config.ChatConstants;
import com.Backend.Chat.dto.Message.MessageDTO;
import com.Backend.Chat.service.implementation.MessageServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final MessageServiceImpl messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{chatId}/send")
    public void sendMessage(@DestinationVariable Long chatId, @Payload MessageDTO messageDTO) {
        log.debug("Received message for chat {}: type={}", chatId, messageDTO.getType());

        messageDTO.setChatId(chatId);
        MessageDTO saved = messageService.saveMessage(messageDTO);

        String destination = ChatConstants.TOPIC_CHAT_PREFIX + chatId;
        messagingTemplate.convertAndSend(destination, saved);

        log.debug("Message {} sent to {}", saved.getId(), destination);
    }
}
