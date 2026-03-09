package com.Backend.Chat.controller;

import com.Backend.Chat.config.ChatConstants;
import com.Backend.Chat.dto.Message.MessageDTO;
import com.Backend.Chat.service.interfaces.IMessageBrokerService;
import com.Backend.Chat.service.interfaces.IMessageHistoryQueryService;
import com.Backend.Chat.service.interfaces.IMessageRealtimeEventService;
import com.Backend.Chat.service.interfaces.IMessageService;
import com.Backend.User.dto.UserDTO;
import com.Backend.User.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final IMessageService messageService;
    private final IUserService userService;
    private final IMessageBrokerService rabbitMQMessageBrokerService;
    private final IMessageHistoryQueryService messageHistoryQueryService;
    private final IMessageRealtimeEventService messageRealtimeEventService;

    @PostMapping
    public ResponseEntity<Void> enqueueMessage(@Valid @RequestBody MessageDTO dto) {
        rabbitMQMessageBrokerService.sendMessage(ChatConstants.ROUTING_KEY_MESSAGE, dto);
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> enqueueEdit(@PathVariable Long id, @Valid @RequestBody MessageDTO edit, Principal principal) {
        UserDTO currentUser = userService.findByUsername(principal.getName());

        if (!messageService.isMessageOwner(id, currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        edit.setId(id);
        edit.setEditorId(currentUser.getId());
        rabbitMQMessageBrokerService.sendMessage(ChatConstants.ROUTING_KEY_EDIT, edit);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> enqueueDelete(@PathVariable Long id, Principal principal) {
        UserDTO currentUser = userService.findByUsername(principal.getName());

        if (!messageService.isMessageOwner(id, currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        rabbitMQMessageBrokerService.sendMessage(ChatConstants.ROUTING_KEY_DELETE, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{chatId}/messages")
    public Page<MessageDTO> getMessageHistory(@PathVariable Long chatId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "sendedAt") String sortBy, @RequestParam(defaultValue = "desc") String direction) {
        PageRequest pageRequest = messageHistoryQueryService.initialize(page, size, sortBy, direction);
        return messageService.getMessagesByChatIdPaginated(chatId, pageRequest);
    }

    @MessageMapping("/chat/{chatId}/read/{messageId}")
    public void markMessageAsRead(@DestinationVariable Long chatId, @DestinationVariable Long messageId, Principal principal) {
        messageRealtimeEventService.initializeReadStatusEvent(chatId, messageId, principal);
    }

    @MessageMapping("/chat/{chatId}/typing")
    public void notifyTyping(@DestinationVariable Long chatId, @Payload(required = false) Map<String, Object> payload, Principal principal) {
        messageRealtimeEventService.initializeTypingEvent(chatId, payload, principal);
    }

    @GetMapping("/unread/{chatId}")
    public ResponseEntity<List<MessageDTO>> getUnreadMessages(Principal principal, @PathVariable Long chatId) {
        UserDTO currentUserDto = userService.findByUsername(principal.getName());

        return ResponseEntity.ok(
                messageService.getUnreadMessages(currentUserDto.getId(), chatId)
        );
    }
}
