package com.Backend.Chat.service.implementation;

import com.Backend.Chat.dto.Chat.ChatDTO;
import com.Backend.Chat.entity.Chat.Chat;
import com.Backend.Chat.Enums.ChatType;
import com.Backend.Chat.entity.Chat.ChatMember;
import com.Backend.Chat.mapper.ChatMapper;
import com.Backend.Chat.repository.ChatMemberRepository;
import com.Backend.Chat.repository.ChatRepository;
import com.Backend.Chat.service.interfaces.IChatService;
import com.Backend.User.entity.User;
import com.Backend.User.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
@Slf4j
public class ChatServiceImpl implements IChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final ChatMapper chatMapper;
    private final ChatMemberRepository chatMemberRepository;
    private final LoadParticipantsService loadParticipantsService;

    public ChatServiceImpl(ChatRepository chatRepository, ChatMemberRepository chatMemberRepository, UserRepository userRepository, ChatMapper chatMapper, LoadParticipantsService loadParticipantsService) {
        this.chatRepository = chatRepository;
        this.chatMemberRepository = chatMemberRepository;
        this.userRepository = userRepository;
        this.chatMapper = chatMapper;
        this.loadParticipantsService = loadParticipantsService;
    }

    @Override
    @Transactional
    public ChatDTO createPrivateChat(Long user1Id, Long user2Id) {
        if (user1Id.equals(user2Id)) {
            throw new IllegalArgumentException("No se puede crear un chat con uno mismo");
        }
        Chat existingChat = chatRepository.findPrivateChatBetweenUsers(user1Id, user2Id).orElse(null);

        validateUserExists(user1Id);
        validateUserExists(user2Id);

        if (existingChat != null) {
            return chatMapper.convertToDTO(existingChat);
        }
        return createNewDirectChat(user1Id, user2Id);
    }

    @Override
    @Transactional
    public ChatDTO getOrCreateDirectChat(Long transmitterId, Long receiverId) {
        if (transmitterId.equals(receiverId)) {
            throw new IllegalArgumentException("No se puede crear un chat con uno mismo");
        }
        validateUserExists(transmitterId);
        validateUserExists(receiverId);

        Optional<Chat> existingChat = chatRepository.findDirectChat(transmitterId, receiverId);
        if (existingChat.isPresent()) {
            return chatMapper.convertToDTO(existingChat.get());
        }

        return createNewDirectChat(transmitterId, receiverId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value="chats", key="#chatId")
    public Optional<ChatDTO> getChatById(Long chatId) {
        return chatRepository.findById(chatId)
                .map(chatMapper::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "chatsByUser", key = "#userId")
    public List<ChatDTO> findAllByUser(Long userId) {
        return chatRepository
                .findAllByMembers_User_Id(userId)
                .stream()
                .map(chatMapper::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "chatByUsername", key = "#username")
    public Optional<ChatDTO> getChatByUsername(String username) {
        return chatRepository.getChatByUsername(username)
                .map(chatMapper::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userHasAccessToChat(String username, Long chatId) {
        return chatMemberRepository.existsByChatIdAndUsername(chatId, username);
    }

    @Transactional(readOnly = true)
    public boolean isUserInChat(Long userId, Long chatId) {
        return chatMemberRepository.existsByChatIdAndUserId(chatId, userId);
    }

    private ChatDTO createNewDirectChat(Long transmitterId, Long receiverId) {
        log.debug("Creating new direct chat between users {} and {}", transmitterId, receiverId);

        List<Long> userIds = List.of(transmitterId, receiverId);
        Set<User> participants = loadParticipantsService.call(userIds);

        Map<Long, User> userMap = participants.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        User transmitter = userMap.get(transmitterId);
        User receiver = userMap.get(receiverId);

        if (transmitter == null || receiver == null) {
            throw new EntityNotFoundException("One or both users not found: transmitter=" + transmitterId + ", receiver=" + receiverId);
        }

        Chat chat = Chat.builder()
                .chatType(ChatType.PRIVATE)
                .chatName("DM: " + transmitter.getUsername() + " & " + receiver.getUsername())
                .isPrivate(true)
                .members(new ArrayList<>())
                .build();

        createMembers(transmitter, receiver, chat);

        Chat createdChat = chatRepository.save(chat);
        log.info("Created new direct chat with id {} between users {} and {}", createdChat.getId(), transmitterId, receiverId);
        return chatMapper.convertToDTO(createdChat);
    }

    private void validateUserExists(Long userId) {
        if (userRepository.findUserById(userId).isEmpty()) {
            throw new EntityNotFoundException("User does not exist with ID: " + userId);
        }
    }

    private void createMembers(User transmitter, User receiver, Chat chat) {
        Date now = new Date();

        ChatMember member1 = new ChatMember();
        member1.setChat(chat);
        member1.setUser(transmitter);
        member1.setJoinedAt(now);

        ChatMember member2 = new ChatMember();
        member2.setChat(chat);
        member2.setUser(receiver);
        member2.setJoinedAt(now);

        chatMemberRepository.saveAll(List.of(member1, member2));
    }
}
