package com.Backend.Chat.dto.Message;

import com.Backend.User.dto.UserDTO;
import com.Backend.Chat.Enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageDTO implements Serializable {
    private Long id;

    @NotNull(message = "El ID del chat es requerido")
    private Long chatId;

    @NotBlank(message = "El contenido del mensaje no puede estar vacío")
    @Size(max = 5000, message = "El mensaje no puede exceder 5000 caracteres")
    private String content;

    private UserDTO sender;

    @NotNull(message = "El tipo de mensaje es requerido")
    private MessageType type;

    private Date sendedAt;

    private Long editorId;
}
