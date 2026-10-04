package com.Backend.Chat.mapper;

import com.Backend.Chat.dto.Message.MessageDTO;
import com.Backend.Chat.entity.Message.Message;
import com.Backend.User.dto.UserDTO;
import jakarta.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    @Autowired
    private ModelMapper modelMapper;

    @PostConstruct
    public void init() {
        modelMapper.getConfiguration()
                .setFieldAccessLevel(Configuration.AccessLevel.PRIVATE)
                .setMatchingStrategy(MatchingStrategies.STANDARD)
                .setSkipNullEnabled(true);
    }

    public MessageDTO convertToDTO(Message message) {
        MessageDTO dto = modelMapper.map(message, MessageDTO.class);
        dto.setContent(message.getMessageContent());
        dto.setChatId(message.getChat().getId());
        dto.setClientMessageId(message.getClientMessageId());
        dto.setDeletedAt(message.getDeletedAt());
        if (message.getSender() != null) {
            UserDTO sender = new UserDTO();
            sender.setId(message.getSender().getId());
            sender.setUsername(message.getSender().getUsername());
            dto.setSender(sender);
        }
        return dto;
    }

    public Message convertToEntity(MessageDTO messageDTO) {
        Message message = modelMapper.map(messageDTO, Message.class);
        message.setMessageContent(messageDTO.getContent());
        return message;
    }
}
