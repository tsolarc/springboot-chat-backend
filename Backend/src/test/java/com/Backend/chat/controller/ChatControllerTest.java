package com.Backend.chat.controller;

import com.Backend.Chat.controller.ChatController;
import com.Backend.Chat.dto.Chat.ChatDTO;
import com.Backend.Chat.dto.Message.MessageDTO;
import com.Backend.Chat.Enums.ChatType;
import com.Backend.Chat.service.implementation.MessageServiceImpl;
import com.Backend.Chat.service.interfaces.IChatService;
import com.Backend.User.dto.UserDTO;
import com.Backend.User.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatControllerTest {

    @Mock
    private IChatService chatService;

    @Mock
    private MessageServiceImpl messageService;

    @Mock
    private UserServiceImpl userService;

    @Mock
    private Principal principal;

    @InjectMocks
    private ChatController chatController;

    private UserDTO currentUser;
    private ChatDTO chatDTO;

    @BeforeEach
    void setUp() {
        currentUser = new UserDTO();
        currentUser.setId(1L);
        currentUser.setUsername("testuser");

        chatDTO = ChatDTO.builder()
                .id(100L)
                .chatName("Test Chat")
                .chatType(ChatType.PRIVATE)
                .memberIds(List.of(1L, 2L))
                .build();

        when(principal.getName()).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(currentUser);
    }

    @Test
    void getOrCreateDmChat_withDifferentUser_shouldReturnChat() {
        when(chatService.getOrCreateDirectChat(1L, 2L)).thenReturn(chatDTO);

        ChatDTO result = chatController.getOrCreateDmChat(principal, 2L);

        assertThat(result).isEqualTo(chatDTO);
        verify(chatService).getOrCreateDirectChat(1L, 2L);
    }

    @Test
    void getOrCreateDmChat_withSameUser_shouldThrowException() {
        assertThatThrownBy(() ->
                chatController.getOrCreateDmChat(principal, 1L)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot create a direct chat with yourself");
    }

    @Test
    void getMessageHistory_shouldReturnMessages() {
        MessageDTO messageDTO = MessageDTO.builder()
                .id(1L)
                .chatId(100L)
                .content("Test message")
                .build();
        List<MessageDTO> messages = List.of(messageDTO);

        when(messageService.getMessagesByChatId(100L)).thenReturn(messages);

        List<MessageDTO> result = chatController.getMessageHistory(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Test message");
        verify(messageService).getMessagesByChatId(100L);
    }

    @Test
    void createPrivateChat_shouldCreateChatWithOtherUser() {
        ChatController.CreateChatRequest request = new ChatController.CreateChatRequest(2L);

        when(chatService.createPrivateChat(1L, 2L)).thenReturn(chatDTO);

        ChatDTO result = chatController.createPrivateChat(principal, request);

        assertThat(result).isEqualTo(chatDTO);
        verify(chatService).createPrivateChat(1L, 2L);
    }

    @Test
    void createPrivateChat_shouldUseAuthenticatedUserAsFirstUser() {
        ChatController.CreateChatRequest request = new ChatController.CreateChatRequest(3L);

        when(chatService.createPrivateChat(1L, 3L)).thenReturn(chatDTO);

        chatController.createPrivateChat(principal, request);

        verify(userService).findByUsername("testuser");
        verify(chatService).createPrivateChat(1L, 3L);
    }
}
