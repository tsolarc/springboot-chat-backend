package com.Backend.Chat.mapper;

import com.Backend.Chat.dto.Chat.ChatDTO;
import com.Backend.Chat.entity.Chat.Chat;
import jakarta.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChatMapper {

    @Autowired
    ModelMapper modelMapper;

    @PostConstruct
    public void init() {
        modelMapper.getConfiguration()
                .setFieldAccessLevel(Configuration.AccessLevel.PRIVATE)
                .setMatchingStrategy(MatchingStrategies.STANDARD)
                .setSkipNullEnabled(true);
    }

    public ChatDTO convertToDTO(Chat chat) {
        ChatDTO chatDTO = modelMapper.map(chat, ChatDTO.class);

        if (chat.getMembers() != null) {
            List<Long> memberIds = chat.getMembers().stream()
                    .map(member -> member.getUser().getId())
                    .collect(Collectors.toList());
            chatDTO.setMemberIds(memberIds);
        } else {
            chatDTO.setMemberIds(new ArrayList<>());
        }
        return chatDTO;
    }

    public Chat convertToEntity(ChatDTO chatDTO) {
        return modelMapper.map(chatDTO, Chat.class);
    }
}
