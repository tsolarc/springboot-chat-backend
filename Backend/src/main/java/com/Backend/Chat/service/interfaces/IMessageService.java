package com.Backend.Chat.service.interfaces;

import com.Backend.Chat.dto.Message.MessageDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import java.util.List;

public interface IMessageService {

    MessageDTO processAndSendMessage(MessageDTO messageDTO);
    MessageDTO saveMessage(MessageDTO messageDTO);
    MessageDTO getMessageById(Long messageId);
    List<MessageDTO> getMessagesByChatId(Long chatId);
    List<MessageDTO> getRecentMessages(Long chatId, int limit);
    MessageDTO editMessage(Long messageId, MessageDTO messageDTO, Long requestingUserId);
    void deleteMessage(Long messageId, Long requestingUserId);
    Page<MessageDTO> getMessagesByChatIdPaginated(Long chatId, Pageable pageable);
    void markAsRead(Long messageId, Long userId);
    List<MessageDTO> getUnreadMessages(Long userId, Long chatId);
    boolean isMessageOwner(Long messageId, Long userId);
}
