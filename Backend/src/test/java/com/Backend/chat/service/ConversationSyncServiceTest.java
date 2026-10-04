package com.Backend.chat.service;

import com.Backend.Chat.entity.Chat.Chat;
import com.Backend.Chat.entity.Chat.ChatMember;
import com.Backend.Chat.entity.Message.Message;
import com.Backend.Chat.exception.ChatAccessDeniedException;
import com.Backend.Chat.mapper.MessageMapper;
import com.Backend.Chat.repository.ChatMemberRepository;
import com.Backend.Chat.repository.ChatRepository;
import com.Backend.Chat.repository.MessageRepository;
import com.Backend.Chat.service.implementation.ConversationSyncService;
import com.Backend.Chat.service.implementation.MessageOutboxWriter;
import com.Backend.Chat.service.implementation.SeekCursor;
import com.Backend.User.entity.User;
import com.Backend.User.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationSyncServiceTest {

    @Mock UserRepository users;
    @Mock ChatRepository chats;
    @Mock ChatMemberRepository members;
    @Mock MessageRepository messages;
    @Mock MessageMapper mapper;
    @Mock MessageOutboxWriter outbox;
    @InjectMocks ConversationSyncService service;

    @Test
    void historyRejectsNonMemberBeforeQueryingMessages() {
        when(users.findByUsername("reader")).thenReturn(Optional.of(User.builder().id(2L).build()));

        assertThatThrownBy(() -> service.messages(7L, null, 20, "reader"))
                .isInstanceOf(ChatAccessDeniedException.class);
        verifyNoInteractions(messages);
    }

    @Test
    void historyReturnsCursorForNextPage() {
        when(users.findByUsername("reader")).thenReturn(Optional.of(User.builder().id(2L).build()));
        when(members.existsByChatIdAndUserId(7L, 2L)).thenReturn(true);
        Message newest = Message.builder().id(12L).sendedAt(new Date(2_000)).build();
        Message older = Message.builder().id(11L).sendedAt(new Date(1_000)).build();
        when(messages.findByChat_IdOrderBySendedAtDescIdDesc(eq(7L), any(Pageable.class)))
                .thenReturn(List.of(newest, older));

        var page = service.messages(7L, null, 1, "reader");

        assertThat(page.nextCursor()).isEqualTo(SeekCursor.encode(newest.getSendedAt(), 12L));
        assertThat(page.messages()).hasSize(1);
    }

    @Test
    void readCursorOnlyAdvancesOnceAndChecksConversation() {
        when(users.findByUsername("reader")).thenReturn(Optional.of(User.builder().id(2L).build()));
        ChatMember member = new ChatMember();
        when(members.findForUpdate(7L, 2L)).thenReturn(member);
        Message message = Message.builder().id(12L).chat(Chat.builder().id(7L).build()).build();
        when(messages.findById(12L)).thenReturn(Optional.of(message));

        assertThat(service.markRead(7L, 12L, "reader").messageId()).isEqualTo(12L);
        assertThat(service.markRead(7L, 12L, "reader").messageId()).isEqualTo(12L);
        verify(outbox, times(1)).readCursorAdvanced(7L, 2L, 12L);
    }
}
