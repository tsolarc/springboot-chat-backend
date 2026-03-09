package com.Backend.Chat.mapper;

import com.Backend.Chat.dto.Message.MessageDTO;
import com.Backend.Chat.entity.Message.Message;
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
        return modelMapper.map(message, MessageDTO.class);
    }

    public Message convertToEntity(MessageDTO messageDTO) {
        return modelMapper.map(messageDTO, Message.class);
    }
}
