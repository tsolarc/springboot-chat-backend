package com.Backend.Chat.service.implementation;

import com.Backend.Chat.dto.Message.MessageDTO;
import com.Backend.Chat.entity.Chat.Chat;
import com.Backend.Chat.entity.Message.Message;
import com.Backend.Chat.entity.Message.MessageHistory;
import com.Backend.Chat.exception.MessageNotFoundException;
import com.Backend.Chat.exception.MessageOwnershipException;
import com.Backend.Chat.mapper.MessageMapper;
import com.Backend.Chat.repository.ChatRepository;
import com.Backend.Chat.repository.MessageHistoryRepository;
import com.Backend.Chat.repository.MessageRepository;
import com.Backend.Chat.service.interfaces.IMessageService;
import com.Backend.User.entity.User;
import com.Backend.User.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements IMessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final MessageHistoryRepository messageHistoryRepository;
    private final MessageMapper messageMapper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public MessageDTO processAndSendMessage(MessageDTO messageDTO) {
        Chat chat = chatRepository.findById(messageDTO.getChatId())
                .orElseThrow(() -> new EntityNotFoundException("Chat not found: " + messageDTO.getChatId()));

        Message message = messageMapper.convertToEntity(messageDTO);
        message.setChat(chat);

        Message savedMessage = messageRepository.save(message);
        return messageMapper.convertToDTO(savedMessage);
    }

    @Transactional
    @Override
    @CacheEvict(value = "recentMessages", key = "#messageDTO.chatId + '*'")
    public MessageDTO saveMessage(MessageDTO messageDTO) {
        Chat chat = chatRepository.findById(messageDTO.getChatId()).orElseThrow(() -> new EntityNotFoundException("Chat not found: " + messageDTO.getChatId()));
        Message message = messageMapper.convertToEntity(messageDTO);
        message.setChat(chat);

        Message saved = messageRepository.save(message);
        return messageMapper.convertToDTO(saved);
    }

    @Override
    public MessageDTO getMessageById(Long messageId) {
        Message message = messageRepository.findById(messageId).orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));
        return messageMapper.convertToDTO(message);
    }

    @Override
    public List<MessageDTO> getMessagesByChatId(Long chatId) {
        return messageRepository.findByChat_IdOrderBySendedAtAsc(chatId).stream()
                .map(messageMapper::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value= "recentMessages", key= "#chatId + '-' + #limit")
    public List<MessageDTO> getRecentMessages(Long chatId, int limit) {
        log.info("Searching for recent messages for the chat {}", chatId);
        return messageRepository.findByChat_IdWithSender(chatId, PageRequest.of(0, limit, Sort.by("sendedAt").descending()))
                .stream()
                .map(messageMapper::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "recentMessages", allEntries = true)
    public MessageDTO editMessage(Long messageId, MessageDTO dto, Long requestingUserId) {
        Message existing = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        if (!isMessageOwner(messageId, requestingUserId)) {
            throw new MessageOwnershipException(requestingUserId, messageId);
        }

        MessageHistory history = new MessageHistory();
        history.setMessage(existing);
        history.setPreviousContent(existing.getMessageContent());
        history.setPreviousType(existing.getType());
        history.setEditedAt(new Date());
        history.setEditor(userRepository.findById(requestingUserId)
                .orElseThrow(() -> new EntityNotFoundException("Editor user not found")));

        messageHistoryRepository.save(history);

        existing.setMessageContent(dto.getContent());
        existing.setType(dto.getType());

        Message updated = messageRepository.save(existing);
        log.info("Mensaje {} editado por usuario {}", messageId, requestingUserId);
        return messageMapper.convertToDTO(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = "recentMessages", allEntries = true)
    public void deleteMessage(Long messageId, Long requestingUserId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));

        if (!isMessageOwner(message.getId(), requestingUserId)) {
            throw new MessageOwnershipException(requestingUserId, messageId);
        }

        messageRepository.deleteById(messageId);
        log.info("Mensaje {} eliminado por usuario {}", messageId, requestingUserId);
    }

    @Override
    public boolean isMessageOwner(Long messageId, Long userId) {
        return messageRepository.findById(messageId)
                .map(message -> message.getSender().getId().equals(userId))
                .orElse(false);
    }

    @Override
    public Page<MessageDTO> getMessagesByChatIdPaginated(Long chatId, Pageable pageable) {
        if (!chatRepository.existsById(chatId)) {
            throw new NoSuchElementException("Chat not found with id: " + chatId);
        }
        Page<Message> messagePage = messageRepository.findByChat_IdWithSender(chatId, pageable);

        return messagePage.map(messageMapper::convertToDTO);
    }

    @Override
    public List<MessageDTO> getUnreadMessages(Long userId, Long chatId) {
        chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Chat not found: " + chatId));

        List<Message> unreadMessages = messageRepository.findUnreadMessages(chatId, userId);

        return unreadMessages.stream()
                .map(messageMapper::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with id: " + messageId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        message.getReadByUsers().put(user, new Date());
        messageRepository.save(message);
    }
}
