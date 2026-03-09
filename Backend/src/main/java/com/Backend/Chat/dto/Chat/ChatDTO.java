package com.Backend.Chat.dto.Chat;

import com.Backend.Chat.Enums.ChatType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.Backend.Chat.dto.Message.MessageDTO;
import com.Backend.Chat.entity.Chat.ChatMember;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatDTO implements Serializable {
    private Long id;

    @Size(max = 100, message = "El nombre del chat no puede exceder 100 caracteres")
    private String chatName;

    @NotNull(message = "El tipo de chat es requerido")
    private ChatType chatType;

    private Date createdAt;

    private List<ChatMember> members;

    private List<MessageDTO> messages;

    @Builder.Default
    private List<Long> memberIds = new ArrayList<>();
}
