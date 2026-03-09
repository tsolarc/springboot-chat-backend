package com.Backend.chat.service;

import com.Backend.Chat.dto.Chat.ChatDTO;
import com.Backend.Chat.dto.Message.MessageDTO;
import com.Backend.Chat.entity.Chat.Chat;
import com.Backend.Chat.entity.Chat.ChatMember;
import com.Backend.Chat.entity.Message.Message;
import com.Backend.Chat.Enums.ChatType;
import com.Backend.Chat.mapper.ChatMapper;
import com.Backend.Chat.mapper.MessageMapper;
import com.Backend.Chat.repository.ChatMemberRepository;
import com.Backend.Chat.repository.ChatRepository;
import com.Backend.Chat.repository.MessageHistoryRepository;
import com.Backend.Chat.repository.MessageRepository;
import com.Backend.Chat.service.implementation.ChatServiceImpl;
import com.Backend.Chat.service.implementation.LoadParticipantsService;
import com.Backend.Chat.service.implementation.MessageServiceImpl;
import com.Backend.TestDataHelper;
import com.Backend.User.dto.UserDTO;
import com.Backend.User.entity.User;
import com.Backend.User.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class ChatServiceTest {

    @Autowired
    private ChatServiceImpl chatService;

    @Autowired
    private MessageServiceImpl messageService;

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final ChatRepository chatRepository = Mockito.mock(ChatRepository.class);
    private final ChatMemberRepository chatMemberRepository = Mockito.mock(ChatMemberRepository.class);
    private final MessageRepository messageRepository = Mockito.mock(MessageRepository.class);
    private final LoadParticipantsService loadParticipantsService = Mockito.mock(LoadParticipantsService.class);
    private final ChatMapper chatMapper = Mockito.mock(ChatMapper.class);
    private final MessageMapper messageMapper = Mockito.mock(MessageMapper.class);
    private final MessageHistoryRepository messageHistoryRepository = Mockito.mock(MessageHistoryRepository.class);

    private User user1;
    private User user2;
    private UserDTO user1DTO;
    private UserDTO user2DTO;
    private ChatDTO chatDTO;
    private Chat chat;

    @BeforeEach
    void setup() {
        user1 = new User();
        user1.setId(1L);
        user1.setUsername("testUser1");
        user1.setEmail("test1@example.com");
        user1.setPassword("password123");

        user2 = new User();
        user2.setId(2L);
        user2.setUsername("testUser2");
        user2.setEmail("test2@example.com");
        user2.setPassword("password123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findByUsername("testUser1")).thenReturn(Optional.of(user1));
        when(userRepository.findByUsername("testUser2")).thenReturn(Optional.of(user2));
        when(userRepository.findUserById(anyLong())).thenReturn(Optional.ofNullable(user1));

        chat = new Chat();
        chat.setId(1L);
        chat.setPrivate(true);
        chat.setChatType(ChatType.PRIVATE);

        List<ChatMember> members = new ArrayList<>();

        ChatMember member1 = new ChatMember();
        member1.setId(1L);
        member1.setChat(chat);
        member1.setUser(user1);
        member1.setJoinedAt(new Date());

        ChatMember member2 = new ChatMember();
        member2.setId(2L);
        member2.setChat(chat);
        member2.setUser(user2);
        member2.setJoinedAt(new Date());

        members.add(member1);
        members.add(member2);
        chat.setMembers(members);

        when(chatRepository.save(any(Chat.class))).thenReturn(chat);
        when(chatRepository.findById(anyLong())).thenReturn(Optional.of(chat));
        when(chatRepository.findPrivateChatBetweenUsers(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(chatRepository.findDirectChat(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(chatRepository.findAllByMembers_User_Id(anyLong())).thenReturn(Collections.singletonList(chat));

        when(chatMemberRepository.save(any(ChatMember.class))).thenReturn(member1);
        when(chatMemberRepository.saveAll(anyList())).thenReturn(Arrays.asList(member1, member2));
        when(chatMemberRepository.findByChatId(anyLong())).thenReturn(Arrays.asList(member1, member2));
        when(chatMemberRepository.existsByChatIdAndUserId(anyLong(), anyLong())).thenReturn(true);
        when(chatMemberRepository.existsByChatIdAndUsername(anyLong(), eq("testUser1"))).thenReturn(true);
        when(chatMemberRepository.existsByChatIdAndUsername(anyLong(), eq("testUser2"))).thenReturn(true);
        when(chatMemberRepository.existsByChatIdAndUsername(anyLong(), eq("testUser3"))).thenReturn(false);

        chatDTO = new ChatDTO();
        chatDTO.setId(1L);
        chatDTO.setChatType(ChatType.PRIVATE);
        List<Long> memberIds = new ArrayList<>();
        memberIds.add(user1.getId());
        memberIds.add(user2.getId());
        chatDTO.setMemberIds(memberIds);

        when(chatMapper.convertToDTO(any(Chat.class))).thenReturn(chatDTO);

        Message message = new Message();
        message.setId(1L);
        message.setChat(chat);
        message.setSender(user1);
        message.setMessageContent("Hola, ¿cómo estás?");
        message.setReadByUsers(new HashMap<>());

        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setId(1L);
        messageDTO.setChatId(1L);
        messageDTO.setSender(user1DTO);
        messageDTO.setContent("Hola, ¿cómo estás?");

        when(messageRepository.save(any(Message.class))).thenReturn(message);
        when(messageMapper.convertToDTO(any(Message.class))).thenReturn(messageDTO);
        when(messageMapper.convertToEntity(any(MessageDTO.class))).thenReturn(message);

        when(messageRepository.findByChatId(anyLong()))
                .thenReturn(Collections.singletonList(message));

        when(messageRepository.findById(anyLong())).thenReturn(Optional.of(message));

        List <Message> messages = TestDataHelper.createMessages(5, chat, user1, user2);
        when(messageRepository.findByChat_Id(anyLong(), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(1);
                    int start = (int) pageable.getOffset();
                    int end = Math.min(start + pageable.getPageSize(), messages.size());
                    List<Message> subList = messages.subList(start, end);
                    return new PageImpl<>(subList, pageable, messages.size());
                });

        when(messageRepository.findByChat_Id(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(new Message())));

        when(messageRepository.findUnreadMessages(anyLong(), anyLong()))
                .thenReturn(Collections.singletonList(message))
                .thenReturn(Collections.emptyList());

        when(messageRepository.findByChatIdOrderBySendedAtDesc(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(message)));

        user1DTO = new UserDTO();
        user1DTO.setId(user1.getId());
        user1DTO.setUsername(user1.getUsername());

        user2DTO = new UserDTO();
        user2DTO.setId(user2.getId());
        user2DTO.setUsername(user2.getUsername());

        when(loadParticipantsService.call(anyList())).thenAnswer(invocation -> {
            List<Long> userIds = invocation.getArgument(0);
            Set<User> participants = new HashSet<>();
            for (Long userId : userIds) {
                if (userId.equals(1L)) {
                    participants.add(user1);
                } else if (userId.equals(2L)) {
                    participants.add(user2);
                }
            }
            return participants;
        });
        this.chatService = new ChatServiceImpl(chatRepository, chatMemberRepository, userRepository, chatMapper, loadParticipantsService);
        this.messageService = new MessageServiceImpl(messageRepository, chatRepository, messageHistoryRepository, messageMapper, userRepository);
    }

    @Test
    public void testCreatePrivateChat() {
        ChatDTO chat = chatService.createPrivateChat(user1.getId(), user2.getId());

        assertNotNull(chat);
        assertNotNull(chat.getId());
        assertEquals(2, chat.getMemberIds().size());
        assertTrue(chat.getMemberIds().contains(user1.getId()));
        assertTrue(chat.getMemberIds().contains(user2.getId()));
        assertTrue(TestDataHelper.validateChatPrivacy(chat));

        Optional<Chat> storedChat = chatRepository.findById(chat.getId());
        assertTrue(storedChat.isPresent());

        List<ChatMember> members = chatMemberRepository.findByChatId(chat.getId());
        assertEquals(2, members.size());
    }

    @Test
    public void testCreatePrivateChatIdempotency() {
        ChatDTO chat1 = chatService.createPrivateChat(user1.getId(), user2.getId());

        ChatDTO chat2 = chatService.createPrivateChat(user1.getId(), user2.getId());

        assertEquals(chat1.getId(), chat2.getId());

        ChatDTO chat3 = chatService.createPrivateChat(user2.getId(), user1.getId());

        assertEquals(chat1.getId(), chat3.getId());
    }

    @Test
    public void testUserHasAccessToChat() {
        ChatDTO chat = chatService.createPrivateChat(user1.getId(), user2.getId());

        assertTrue(chatService.userHasAccessToChat(user1.getUsername(), chat.getId()));
        assertTrue(chatService.userHasAccessToChat(user2.getUsername(), chat.getId()));

        User user3 = new User();
        user3.setUsername("testUser3");
        user3.setEmail("test3@example.com");
        user3.setPassword("password123");
        userRepository.save(user3);

        assertFalse(chatService.userHasAccessToChat(user3.getUsername(), chat.getId()));
    }

    @Test
    public void validateIfMessageReadStatus() {
        ChatDTO chat = chatService.createPrivateChat(user1.getId(), user2.getId());

        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setChatId(chat.getId());
        messageDTO.setSender(user1DTO);
        messageDTO.setContent("Hola, ¿cómo estás?");

        MessageDTO savedMessage = messageService.saveMessage(messageDTO);

        List<MessageDTO> unreadMessages = messageService.getUnreadMessages(user2.getId(), chat.getId());
        assertEquals(1, unreadMessages.size());

        messageService.markAsRead(savedMessage.getId(), user2.getId());

        unreadMessages = messageService.getUnreadMessages(user2.getId(), chat.getId());
        assertEquals(0, unreadMessages.size());

        Message message = messageRepository.findById(savedMessage.getId()).orElseThrow();
        assertTrue(message.getReadByUsers().containsKey(user2));
    }

    @Test
    void testChatMembershipConsistency() {
        when(chatMapper.convertToDTO(any(Chat.class))).thenReturn(chatDTO);

        ChatDTO chat = chatService.createPrivateChat(user1.getId(), user2.getId());

        assertNotNull(chat, "El chat creado no debería ser nulo");
        assertNotNull(chat.getId(), "El ID del chat no debería ser nulo");

        List<ChatMember> membersList = new ArrayList<>();

        ChatMember member1 = new ChatMember();
        member1.setId(1L);
        member1.setChat(this.chat);
        member1.setUser(user1);
        member1.setJoinedAt(new Date());

        ChatMember member2 = new ChatMember();
        member2.setId(2L);
        member2.setChat(this.chat);
        member2.setUser(user2);
        member2.setJoinedAt(new Date());

        membersList.add(member1);
        membersList.add(member2);

        when(chatMemberRepository.findByChatId(chat.getId())).thenReturn(membersList);

        List<ChatMember> members = chatMemberRepository.findByChatId(chat.getId());
        assertEquals(2, members.size());

        boolean user1IsMember = members.stream()
                .anyMatch(m -> m.getUser().getId().equals(user1.getId()));
        boolean user2IsMember = members.stream()
                .anyMatch(m -> m.getUser().getId().equals(user2.getId()));

        assertTrue(user1IsMember);
        assertTrue(user2IsMember);

        assertNotNull(members.get(0).getJoinedAt());
        assertNotNull(members.get(1).getJoinedAt());
    }

    @Test
    void createPrivateChat_withSameUser_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            chatService.createPrivateChat(user1.getId(), user1.getId());
        });
    }

    @Test
    void createPrivateChat_withNonExistentUser_throwsEntityNotFoundException() {
        when(userRepository.findUserById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            chatService.createPrivateChat(user1.getId(), 99L);
        });
    }

    @Test
    void getOrCreateDirectChat_whenChatExists_returnsExistingChat() {
        when(chatRepository.findDirectChat(user1.getId(), user2.getId())).thenReturn(Optional.of(chat));

        ChatDTO result = chatService.getOrCreateDirectChat(user1.getId(), user2.getId());

        assertNotNull(result);
        assertEquals(chatDTO.getId(), result.getId());
        verify(chatRepository, never()).save(any(Chat.class));
    }

    @Test
    void getOrCreateDirectChat_whenChatDoesNotExist_createsNewChat() {
        when(chatRepository.findDirectChat(user1.getId(), user2.getId())).thenReturn(Optional.empty());

        ChatDTO result = chatService.getOrCreateDirectChat(user1.getId(), user2.getId());

        assertNotNull(result);
        verify(chatRepository).save(any(Chat.class));
        verify(chatMemberRepository).saveAll(anyList());
    }

    @Test
    void findAllByUser_whenUserHasChats_returnsChatList() {
        when(chatRepository.findAllByMembers_User_Id(user1.getId())).thenReturn(Collections.singletonList(chat));

        List<ChatDTO> result = chatService.findAllByUser(user1.getId());

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(chatDTO.getId(), result.get(0).getId());
    }

    @Test
    void findAllByUser_whenUserHasNoChats_returnsEmptyList() {
        when(chatRepository.findAllByMembers_User_Id(anyLong())).thenReturn(Collections.emptyList());

        List<ChatDTO> result = chatService.findAllByUser(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void isUserInChat_whenUserIsMember_returnsTrue() {
        when(chatMemberRepository.existsByChatIdAndUserId(chat.getId(), user1.getId())).thenReturn(true);

        boolean result = chatService.isUserInChat(user1.getId(), chat.getId());

        assertTrue(result);
    }

    @Test
    void isUserInChat_whenUserIsNotMember_returnsFalse() {
        when(chatMemberRepository.existsByChatIdAndUserId(chat.getId(), 99L)).thenReturn(false);

        boolean result = chatService.isUserInChat(99L, chat.getId());

        assertFalse(result);
    }
}
