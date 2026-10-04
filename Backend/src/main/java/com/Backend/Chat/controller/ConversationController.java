package com.Backend.Chat.controller;

import com.Backend.Chat.dto.Chat.ChatDTO;
import com.Backend.Chat.dto.Chat.ConversationPage;
import com.Backend.Chat.dto.Message.ReadCursorRequest;
import com.Backend.Chat.dto.Message.ReadCursorResponse;
import com.Backend.Chat.service.implementation.ConversationSyncService;
import com.Backend.Chat.service.interfaces.IChatService;
import com.Backend.User.service.IUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/conversations")
public class ConversationController {

    private final ConversationSyncService syncService;
    private final IChatService chatService;
    private final IUserService userService;

    public record CreateDirectRequest(@NotNull Long otherUserId) {}

    @GetMapping
    public ConversationPage list(@RequestParam(required = false) String before,
                                 @RequestParam(defaultValue = "20") int limit,
                                 Principal principal) {
        return syncService.conversations(before, limit, principal.getName());
    }

    @PostMapping("/direct")
    public ChatDTO direct(@Valid @RequestBody CreateDirectRequest request, Principal principal) {
        Long userId = userService.findByUsername(principal.getName()).getId();
        return chatService.getOrCreateDirectChat(userId, request.otherUserId());
    }

    @PutMapping("/{chatId}/read-cursor")
    public ReadCursorResponse read(@PathVariable Long chatId,
                                   @Valid @RequestBody ReadCursorRequest request,
                                   Principal principal) {
        return syncService.markRead(chatId, request.messageId(), principal.getName());
    }
}
