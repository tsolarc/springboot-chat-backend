package com.Backend.chat.controller;

import com.Backend.Chat.config.ChatConstants;
import com.Backend.Chat.service.implementation.MessageCommandService;
import com.Backend.Chat.service.interfaces.IChatService;
import com.Backend.Chat.controller.MessageController;
import com.Backend.Chat.dto.Message.MessageDTO;
import com.Backend.Chat.Enums.MessageType;
import com.Backend.Chat.service.interfaces.IMessageBrokerService;
import com.Backend.Chat.service.interfaces.IMessageHistoryQueryService;
import com.Backend.Chat.service.interfaces.IMessageRealtimeEventService;
import com.Backend.Chat.service.interfaces.IMessageService;
import com.Backend.User.dto.UserDTO;
import com.Backend.User.service.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageControllerTest {

    @Mock
    private IMessageService messageService;

    @Mock
    private IUserService userService;

    @Mock
    private MessageCommandService messageCommandService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private IChatService chatService;

    @Mock
    private IMessageHistoryQueryService messageHistoryQueryService;

    @Mock
    private IMessageRealtimeEventService messageRealtimeEventService;

    @Mock
    private Principal principal;

    @InjectMocks
    private MessageController messageController;

    private MessageDTO messageDTO;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        messageDTO = MessageDTO.builder()
                .id(1L)
                .chatId(100L)
                .content("Test message")
                .type(MessageType.TEXT)
                .clientMessageId(UUID.randomUUID())
                .build();

        userDTO = new UserDTO();
        userDTO.setId(5L);
        userDTO.setUsername("testuser");

        when(principal.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(userDTO);
        when(chatService.userHasAccessToChat("testuser", 100L)).thenReturn(true);
    }

    @Test
    void enqueueMessage_shouldPersistAndPublish() {
        when(messageCommandService.send(eq(100L), any(), eq("testuser")))
                .thenReturn(new MessageCommandService.SendResult(messageDTO, true));
        ResponseEntity<MessageDTO> response = messageController.enqueueMessage(messageDTO, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(messageCommandService).send(eq(100L), any(), eq("testuser"));
    }

    @Test
    void enqueueEdit_whenOwner_shouldSendEditMessage() {
        when(messageService.isMessageOwner(1L, 5L)).thenReturn(true);

        MessageDTO editDTO = MessageDTO.builder()
                .content("Edited content")
                .type(MessageType.TEXT)
                .build();

        when(messageService.editMessage(1L, editDTO, 5L)).thenReturn(messageDTO);
        ResponseEntity<MessageDTO> response = messageController.enqueueEdit(1L, editDTO, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(messageService).editMessage(1L, editDTO, 5L);
    }

    @Test
    void enqueueEdit_whenNotOwner_shouldReturnForbidden() {
        when(messageService.isMessageOwner(1L, 5L)).thenReturn(false);

        MessageDTO editDTO = MessageDTO.builder()
                .content("Edited content")
                .type(MessageType.TEXT)
                .build();

        ResponseEntity<MessageDTO> response = messageController.enqueueEdit(1L, editDTO, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(messageService, never()).editMessage(any(), any(), any());
    }

    @Test
    void enqueueDelete_whenOwner_shouldSendDeleteMessage() {
        when(messageService.isMessageOwner(1L, 5L)).thenReturn(true);
        when(messageService.getMessageById(1L)).thenReturn(messageDTO);

        ResponseEntity<Void> response = messageController.enqueueDelete(1L, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(messageService).deleteMessage(1L, 5L);
    }

    @Test
    void enqueueDelete_whenNotOwner_shouldReturnForbidden() {
        when(messageService.isMessageOwner(1L, 5L)).thenReturn(false);

        ResponseEntity<Void> response = messageController.enqueueDelete(1L, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(messageService, never()).deleteMessage(any(), any());
    }

    @Test
    void getMessageHistory_withValidSortField_shouldReturnMessages() {
        Page<MessageDTO> messagePage = new PageImpl<>(List.of(messageDTO));
        PageRequest pageRequest = PageRequest.of(0, 20);

        when(messageHistoryQueryService.initialize(0, 20, "sendedAt", "desc")).thenReturn(pageRequest);
        when(messageService.getMessagesByChatIdPaginated(100L, pageRequest))
                .thenReturn(messagePage);

        Page<MessageDTO> result = messageController.getMessageHistory(100L, 0, 20, "sendedAt", "desc", principal);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(messageDTO);
    }

    @Test
    void getMessageHistory_withInvalidSortField_shouldThrowException() {
        when(messageHistoryQueryService.initialize(0, 20, "invalidField", "desc"))
                .thenThrow(new IllegalArgumentException("Invalid sort field: invalidField"));

        assertThatThrownBy(() ->
                messageController.getMessageHistory(100L, 0, 20, "invalidField", "desc", principal)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort field");
    }

    @Test
    void getMessageHistory_withInvalidDirection_shouldThrowException() {
        when(messageHistoryQueryService.initialize(0, 20, "id", "upward"))
                .thenThrow(new IllegalArgumentException("Invalid sort direction: upward"));

        assertThatThrownBy(() ->
                messageController.getMessageHistory(100L, 0, 20, "id", "upward", principal)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort direction");
    }

    @Test
    void getMessageHistory_withInvalidPage_shouldThrowException() {
        when(messageHistoryQueryService.initialize(-1, 20, "id", "desc"))
                .thenThrow(new IllegalArgumentException("Page must be greater than or equal to 0"));

        assertThatThrownBy(() ->
                messageController.getMessageHistory(100L, -1, 20, "id", "desc", principal)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page must be greater than or equal to 0");
    }

    @Test
    void getMessageHistory_withInvalidSize_shouldThrowException() {
        when(messageHistoryQueryService.initialize(0, 0, "id", "desc"))
                .thenThrow(new IllegalArgumentException("Size must be between 1 and 100"));

        assertThatThrownBy(() ->
                messageController.getMessageHistory(100L, 0, 0, "id", "desc", principal)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Size must be between 1 and 100");
    }

    @Test
    void getMessageHistory_withAscDirection_shouldSortAscending() {
        Page<MessageDTO> messagePage = new PageImpl<>(List.of(messageDTO));
        PageRequest pageRequest = PageRequest.of(0, 20);

        when(messageHistoryQueryService.initialize(0, 20, "id", "asc")).thenReturn(pageRequest);
        when(messageService.getMessagesByChatIdPaginated(100L, pageRequest))
                .thenReturn(messagePage);

        Page<MessageDTO> result = messageController.getMessageHistory(100L, 0, 20, "id", "asc", principal);

        assertThat(result).isNotNull();
        verify(messageService).getMessagesByChatIdPaginated(eq(100L), any(PageRequest.class));
    }

    @Test
    void getUnreadMessages_shouldReturnUnreadMessages() {
        List<MessageDTO> unreadMessages = List.of(messageDTO);

        when(messageService.getUnreadMessages(5L, 100L)).thenReturn(unreadMessages);

        ResponseEntity<List<MessageDTO>> response = messageController.getUnreadMessages(principal, 100L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(messageService).getUnreadMessages(5L, 100L);
    }

    @Test
    void markMessageAsRead_withNullPrincipal_shouldNotProcess() {
        messageController.markMessageAsRead(100L, 1L, null);

        verify(messageRealtimeEventService).initializeReadStatusEvent(100L, 1L, null);
    }

    @Test
    void markMessageAsRead_withValidPrincipal_shouldMarkAndNotify() {
        messageController.markMessageAsRead(100L, 1L, principal);

        verify(messageRealtimeEventService).initializeReadStatusEvent(100L, 1L, principal);
    }

    @Test
    void notifyTyping_withNullPrincipal_shouldNotProcess() {
        messageController.notifyTyping(100L, null, null);

        verify(messageRealtimeEventService).initializeTypingEvent(100L, null, null);
    }

    @Test
    void notifyTyping_withValidPrincipal_shouldSendTypingNotification() {
        messageController.notifyTyping(100L, null, principal);

        verify(messageRealtimeEventService).initializeTypingEvent(100L, null, principal);
    }
}
