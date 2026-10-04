package com.Backend.chat.service;

import com.Backend.Chat.dto.Message.SendMessageRequest;
import com.Backend.Chat.entity.Chat.Chat;
import com.Backend.Chat.entity.Message.Message;
import com.Backend.Chat.exception.ChatAccessDeniedException;
import com.Backend.Chat.repository.ChatMemberRepository;
import com.Backend.Chat.repository.ChatRepository;
import com.Backend.Chat.repository.MessageRepository;
import com.Backend.Chat.service.implementation.MessageCommandService;
import com.Backend.Chat.service.implementation.MessageOutboxWriter;
import com.Backend.User.entity.User;
import com.Backend.User.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageCommandServiceTest {

    @Mock MessageRepository messages;
    @Mock ChatRepository chats;
    @Mock ChatMemberRepository members;
    @Mock UserRepository users;
    @Mock MessageOutboxWriter outbox;
    @InjectMocks MessageCommandService service;

    private User sender;
    private Chat chat;
    private SendMessageRequest request;

    @BeforeEach
    void setUp() {
        sender = User.builder().id(7L).username("sender").build();
        chat = Chat.builder().id(15L).build();
        request = new SendMessageRequest(UUID.randomUUID(), "hello");
    }

    @Test
    void firstSendPersistsMessageAndOutbox() {
        when(users.findByUsernameForUpdate("sender")).thenReturn(Optional.of(sender));
        when(members.existsByChatIdAndUserId(15L, 7L)).thenReturn(true);
        when(messages.findBySender_IdAndClientMessageId(7L, request.clientMessageId())).thenReturn(Optional.empty());
        when(chats.findById(15L)).thenReturn(Optional.of(chat));
        when(messages.saveAndFlush(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            message.setId(50L);
            message.setSendedAt(new Date());
            return message;
        });

        MessageCommandService.SendResult result = service.send(15L, request, "sender");

        assertThat(result.created()).isTrue();
        assertThat(result.message().getSender().getId()).isEqualTo(7L);
        assertThat(result.message().getClientMessageId()).isEqualTo(request.clientMessageId());
        verify(outbox).messageCreated(any(Message.class));
        verify(chats).advanceLastActivity(eq(15L), any(Date.class));
    }

    @Test
    void retryReturnsStoredMessageWithoutSecondEvent() {
        Message existing = Message.builder().id(50L).chat(chat).sender(sender)
                .messageContent("hello").clientMessageId(request.clientMessageId()).sendedAt(new Date()).build();
        when(users.findByUsernameForUpdate("sender")).thenReturn(Optional.of(sender));
        when(members.existsByChatIdAndUserId(15L, 7L)).thenReturn(true);
        when(messages.findBySender_IdAndClientMessageId(7L, request.clientMessageId())).thenReturn(Optional.of(existing));

        MessageCommandService.SendResult result = service.send(15L, request, "sender");

        assertThat(result.created()).isFalse();
        assertThat(result.message().getId()).isEqualTo(50L);
        verify(messages, never()).saveAndFlush(any());
        verifyNoInteractions(outbox);
    }

    @Test
    void nonMemberCannotSend() {
        when(users.findByUsernameForUpdate("sender")).thenReturn(Optional.of(sender));
        when(members.existsByChatIdAndUserId(15L, 7L)).thenReturn(false);

        assertThatThrownBy(() -> service.send(15L, request, "sender"))
                .isInstanceOf(ChatAccessDeniedException.class);
        verifyNoInteractions(messages, outbox);
    }
}
