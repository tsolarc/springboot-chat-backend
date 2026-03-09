package com.Backend.chat.mapper;

import com.Backend.Chat.dto.Message.MessageDTO;
import com.Backend.Chat.entity.Chat.Chat;
import com.Backend.Chat.entity.Message.Message;
import com.Backend.Chat.Enums.MessageType;
import com.Backend.Chat.mapper.MessageMapper;
import com.Backend.User.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageMapperTest {

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private MessageMapper messageMapper;

    private Message message;
    private MessageDTO messageDTO;
    private User sender;
    private Chat chat;

    @BeforeEach
    void setUp() {
        sender = User.builder()
                .id(1L)
                .username("testuser")
                .build();

        chat = Chat.builder()
                .id(100L)
                .chatName("Test Chat")
                .build();

        message = Message.builder()
                .id(1L)
                .messageContent("Hello World")
                .type(MessageType.TEXT)
                .sender(sender)
                .chat(chat)
                .sendedAt(new Date())
                .build();

        messageDTO = MessageDTO.builder()
                .id(1L)
                .content("Hello World")
                .type(MessageType.TEXT)
                .chatId(100L)
                .build();
    }

    @Test
    void convertToDTO_shouldMapMessageToDTO() {
        when(modelMapper.map(any(Message.class), eq(MessageDTO.class))).thenReturn(messageDTO);

        MessageDTO result = messageMapper.convertToDTO(message);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("Hello World");
        assertThat(result.getType()).isEqualTo(MessageType.TEXT);
    }

    @Test
    void convertToEntity_shouldMapDTOToMessage() {
        when(modelMapper.map(any(MessageDTO.class), eq(Message.class))).thenReturn(message);

        Message result = messageMapper.convertToEntity(messageDTO);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getMessageContent()).isEqualTo("Hello World");
    }

    @Test
    void convertToDTO_withDifferentMessageTypes_shouldMapCorrectly() {
        message.setType(MessageType.IMAGE);
        MessageDTO imageMsgDTO = MessageDTO.builder()
                .id(1L)
                .type(MessageType.IMAGE)
                .build();

        when(modelMapper.map(any(Message.class), eq(MessageDTO.class))).thenReturn(imageMsgDTO);

        MessageDTO result = messageMapper.convertToDTO(message);

        assertThat(result.getType()).isEqualTo(MessageType.IMAGE);
    }
}
