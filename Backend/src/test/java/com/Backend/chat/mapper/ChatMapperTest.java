package com.Backend.chat.mapper;

import com.Backend.Chat.dto.Chat.ChatDTO;
import com.Backend.Chat.entity.Chat.Chat;
import com.Backend.Chat.entity.Chat.ChatMember;
import com.Backend.Chat.Enums.ChatType;
import com.Backend.Chat.mapper.ChatMapper;
import com.Backend.User.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMapperTest {

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ChatMapper chatMapper;

    private Chat chat;
    private ChatDTO chatDTO;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = User.builder().id(1L).username("user1").build();
        user2 = User.builder().id(2L).username("user2").build();

        ChatMember member1 = ChatMember.builder()
                .id(10L)
                .user(user1)
                .joinedAt(new Date())
                .build();

        ChatMember member2 = ChatMember.builder()
                .id(11L)
                .user(user2)
                .joinedAt(new Date())
                .build();

        chat = Chat.builder()
                .id(100L)
                .chatName("Test Chat")
                .chatType(ChatType.PRIVATE)
                .isPrivate(true)
                .members(new ArrayList<>(List.of(member1, member2)))
                .createdAt(new Date())
                .build();

        member1.setChat(chat);
        member2.setChat(chat);

        chatDTO = ChatDTO.builder()
                .id(100L)
                .chatName("Test Chat")
                .chatType(ChatType.PRIVATE)
                .build();
    }

    @Test
    void convertToDTO_shouldMapChatToDTO() {
        when(modelMapper.map(any(Chat.class), eq(ChatDTO.class))).thenReturn(chatDTO);

        ChatDTO result = chatMapper.convertToDTO(chat);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getChatName()).isEqualTo("Test Chat");
    }

    @Test
    void convertToDTO_shouldIncludeMemberIds() {
        when(modelMapper.map(any(Chat.class), eq(ChatDTO.class))).thenReturn(chatDTO);

        ChatDTO result = chatMapper.convertToDTO(chat);

        assertThat(result.getMemberIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void convertToDTO_withNullMembers_shouldReturnEmptyList() {
        chat.setMembers(null);
        when(modelMapper.map(any(Chat.class), eq(ChatDTO.class))).thenReturn(chatDTO);

        ChatDTO result = chatMapper.convertToDTO(chat);

        assertThat(result.getMemberIds()).isEmpty();
    }

    @Test
    void convertToDTO_withEmptyMembers_shouldReturnEmptyList() {
        chat.setMembers(new ArrayList<>());
        when(modelMapper.map(any(Chat.class), eq(ChatDTO.class))).thenReturn(chatDTO);

        ChatDTO result = chatMapper.convertToDTO(chat);

        assertThat(result.getMemberIds()).isEmpty();
    }

    @Test
    void convertToEntity_shouldMapDTOToChat() {
        when(modelMapper.map(any(ChatDTO.class), eq(Chat.class))).thenReturn(chat);

        Chat result = chatMapper.convertToEntity(chatDTO);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
    }
}
