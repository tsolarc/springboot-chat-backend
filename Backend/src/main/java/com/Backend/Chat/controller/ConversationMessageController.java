package com.Backend.Chat.controller;

import com.Backend.Chat.dto.Message.MessageDTO;
import com.Backend.Chat.dto.Message.SendMessageRequest;
import com.Backend.Chat.dto.Message.MessagePage;
import com.Backend.Chat.service.implementation.MessageCommandService;
import com.Backend.Chat.service.implementation.ConversationSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/conversations/{chatId}/messages")
public class ConversationMessageController {

    private final MessageCommandService messageCommandService;
    private final ConversationSyncService conversationSyncService;

    @GetMapping
    public MessagePage history(@PathVariable Long chatId,
                               @RequestParam(required = false) String before,
                               @RequestParam(defaultValue = "20") int limit,
                               Principal principal) {
        return conversationSyncService.messages(chatId, before, limit, principal.getName());
    }

    @PostMapping
    public ResponseEntity<MessageDTO> send(@PathVariable Long chatId,
                                           @Valid @RequestBody SendMessageRequest request,
                                           Principal principal) {
        MessageCommandService.SendResult result = messageCommandService.send(chatId, request, principal.getName());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.message());
    }
}
