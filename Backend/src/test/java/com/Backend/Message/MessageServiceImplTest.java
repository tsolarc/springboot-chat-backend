package com.Backend.Message;

import com.Backend.Chat.dto.Message.MessageDTO;
import com.Backend.Chat.entity.Chat.Chat;
import com.Backend.Chat.entity.Message.Message;
import com.Backend.Chat.Enums.MessageType;
import com.Backend.Chat.exception.MessageNotFoundException;
import com.Backend.Chat.exception.MessageOwnershipException;
import com.Backend.Chat.mapper.MessageMapper;
import com.Backend.Chat.repository.ChatRepository;
import com.Backend.Chat.repository.MessageHistoryRepository;
import com.Backend.Chat.repository.MessageRepository;
import com.Backend.Chat.service.implementation.MessageServiceImpl;
import com.Backend.User.entity.User;
import com.Backend.User.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageServiceImplTest {

    @Mock
    MessageRepository messageRepository;

    @Mock
    ChatRepository chatRepository;

    @Mock
    MessageHistoryRepository messageHistoryRepository;

    @Mock
    MessageMapper messageMapper;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    MessageServiceImpl messageService;

    Chat chat;
    Message message;
    MessageDTO messageDTO;
    User sender;

    @BeforeEach
    void setUp() {
        sender = User.builder()
                .id(5L)
                .username("testuser")
                .build();

        chat = Chat.builder()
                .id(100L)
                .build();

        message = Message.builder()
                .id(1L)
                .messageContent("Hola mundo")
                .type(MessageType.TEXT)
                .chat(chat)
                .sender(sender)
                .build();

        messageDTO = MessageDTO.builder()
                .id(1L)
                .chatId(100L)
                .content("Hola mundo")
                .type(MessageType.TEXT)
                .build();
    }

    @Test
    void saveMessage_happyPath() {
        when(chatRepository.findById(100L))
                .thenReturn(Optional.of(chat));
        when(messageMapper.convertToEntity(messageDTO))
                .thenReturn(message);
        when(messageRepository.save(message))
                .thenReturn(message);
        when(messageMapper.convertToDTO(message))
                .thenReturn(messageDTO);

        MessageDTO res = messageService.saveMessage(messageDTO);

        assertThat(res).isEqualTo(messageDTO);

        InOrder ord = inOrder(chatRepository, messageMapper, messageRepository);
        ord.verify(chatRepository).findById(100L);
        ord.verify(messageMapper).convertToEntity(messageDTO);
        ord.verify(messageRepository).save(message);
        ord.verify(messageMapper).convertToDTO(message);
    }

    @Test
    void saveMessage_chatNotFound_throws() {
        when(chatRepository.findById(100L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                messageService.saveMessage(messageDTO)
        ).isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Chat not found: 100");
    }

    @Test
    void getMessageById_happyPath() {
        when(messageRepository.findById(1L))
                .thenReturn(Optional.of(message));
        when(messageMapper.convertToDTO(message))
                .thenReturn(messageDTO);

        MessageDTO res = messageService.getMessageById(1L);

        assertThat(res).isEqualTo(messageDTO);
        verify(messageRepository).findById(1L);
        verify(messageMapper).convertToDTO(message);
    }

    @Test
    void getMessageById_notFound_throws() {
        when(messageRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                messageService.getMessageById(1L)
        ).isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Message not found: 1");
    }

    @Test
    void getMessagesByChatId_happyPath() {
        List<Message> lista = List.of(message);
        when(messageRepository.findByChat_IdOrderBySendedAtAsc(100L))
                .thenReturn(lista);
        when(messageMapper.convertToDTO(message))
                .thenReturn(messageDTO);

        List<MessageDTO> res = messageService.getMessagesByChatId(100L);

        assertThat(res).containsExactly(messageDTO);
        verify(messageRepository).findByChat_IdOrderBySendedAtAsc(100L);
        verify(messageMapper).convertToDTO(message);
    }

    @Test
    void getRecentMessages_happyPath() {
        List<Message> messages = List.of(message);
        PageRequest page = PageRequest.of(0, 5, Sort.by("sendedAt").descending());

        Page<Message> messagePage = new PageImpl<>(messages, page, messages.size());

        when(messageRepository.findByChat_IdWithSender(100L, page))
                .thenReturn(messagePage);
        when(messageMapper.convertToDTO(message))
                .thenReturn(messageDTO);

        List<MessageDTO> response = messageService.getRecentMessages(100L, 5);

        assertThat(response).containsExactly(messageDTO);
        verify(messageRepository).findByChat_IdWithSender(100L, page);
        verify(messageMapper).convertToDTO(message);
    }

    @Test
    void deleteMessage_whenOwner_deletes() {
        when(messageRepository.findById(1L))
                .thenReturn(Optional.of(message));

        messageService.deleteMessage(1L, 5L);

        verify(messageRepository).deleteById(1L);
    }

    @Test
    void deleteMessage_notExists_throws() {
        when(messageRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                messageService.deleteMessage(1L, 5L)
        ).isInstanceOf(MessageNotFoundException.class);
    }

    @Test
    void deleteMessage_notOwner_throws() {
        when(messageRepository.findById(1L))
                .thenReturn(Optional.of(message));

        assertThatThrownBy(() ->
                messageService.deleteMessage(1L, 999L)
        ).isInstanceOf(MessageOwnershipException.class);
    }

    @Test
    void editMessage_happyPath() {
        Message updated = Message.builder()
                .id(1L)
                .messageContent("Editado")
                .type(MessageType.TEXT)
                .chat(chat)
                .build();
        MessageDTO updatedDto = MessageDTO.builder()
                .id(1L)
                .chatId(100L)
                .content("Editado")
                .type(MessageType.TEXT)
                .build();

        when(messageRepository.findById(1L))
                .thenReturn(Optional.of(message));
        when(messageRepository.save(message))
                .thenReturn(updated);
        when(messageMapper.convertToDTO(updated))
                .thenReturn(updatedDto);
    }

    @Test
    void validate_markAsRead_happyPath() {
        User reader = User.builder().id(5L).build();
        message.setReadByUsers(new HashMap<>());

        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(userRepository.findById(5L)).thenReturn(Optional.of(reader));

        messageService.markAsRead(1L, 5L);

        verify(messageRepository).findById(1L);
        verify(userRepository).findById(5L);
        verify(messageRepository).save(message);

        assertThat(message.getReadByUsers()).containsKey(reader);
    }

    @Test
    void markAsRead_messageNotFound_throws() {
        when(messageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                messageService.markAsRead(1L, 5L)
        ).isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Message not found");
    }

    @Test
    void markAsRead_userNotFound_throws() {
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                messageService.markAsRead(1L, 5L)
        ).isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getUnreadMessages_happyPath() {
        Message unreadMessage = Message.builder().id(2L).build();
        List<Message> unreadList = List.of(unreadMessage);
        MessageDTO unreadDto = MessageDTO.builder().id(2L).build();

        when(messageRepository.findUnreadMessages(100L, 5L)).thenReturn(unreadList);
        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));
        when(messageMapper.convertToDTO(unreadMessage)).thenReturn(unreadDto);

        List<MessageDTO> result = messageService.getUnreadMessages(5L, 100L);

        verify(messageRepository).findUnreadMessages(100L, 5L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(2L);
    }

    @Test
    void getUnreadMessages_noUnreadMessages_returnsEmptyList() {
        when(messageRepository.findUnreadMessages(100L, 5L)).thenReturn(Collections.emptyList());
        when(chatRepository.findById(100L)).thenReturn(Optional.of(chat));

        List<MessageDTO> result = messageService.getUnreadMessages(5L, 100L);

        verify(messageRepository).findUnreadMessages(100L, 5L);
        assertThat(result).isEmpty();
    }

    @Test
    void markAsRead_alreadyRead_doesNotDuplicate() {
        User reader = User.builder().id(5L).build();
        Date date = new Date();
        message.setReadByUsers(new HashMap<>());
        message.getReadByUsers().put(reader, date);

        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(userRepository.findById(5L)).thenReturn(Optional.of(reader));

        messageService.markAsRead(1L, 5L);

        assertThat(message.getReadByUsers()).hasSize(1);
    }
}
