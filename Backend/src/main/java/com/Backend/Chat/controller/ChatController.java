package com.Backend.Chat.controller;

import com.Backend.Chat.dto.Chat.ChatDTO;
import com.Backend.Chat.dto.Message.MessageDTO;
import com.Backend.Chat.service.implementation.MessageServiceImpl;
import com.Backend.Chat.service.interfaces.IChatService;
import com.Backend.User.dto.UserDTO;
import com.Backend.User.service.UserServiceImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final IChatService chatService;
    private final MessageServiceImpl messageService;
    private final UserServiceImpl userService;

    @GetMapping("/direct/{submitterUserId}")
    public ChatDTO getOrCreateDmChat(Principal principal, @PathVariable Long submitterUserId) {
        UserDTO currentUser = userService.findByUsername(principal.getName());
        if (currentUser.getId().equals(submitterUserId)) {
            throw new IllegalArgumentException("You cannot create a direct chat with yourself.");
        }
        return chatService.getOrCreateDirectChat(currentUser.getId(), submitterUserId);
    }

    @GetMapping("/{chatId}/messages")
    public List<MessageDTO> getMessageHistory(@PathVariable Long chatId) {
        return messageService.getMessagesByChatId(chatId);
    }

    @PostMapping
    public ChatDTO createPrivateChat(Principal principal, @Valid @RequestBody CreateChatRequest request) {
        UserDTO currentUser = userService.findByUsername(principal.getName());
        return chatService.createPrivateChat(currentUser.getId(), request.otherUserId());
    }

    public record CreateChatRequest(
            @NotNull(message = "otherUserId is required")
            Long otherUserId
    ) {}
}
