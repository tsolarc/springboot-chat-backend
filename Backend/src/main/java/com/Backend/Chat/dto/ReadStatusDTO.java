package com.Backend.Chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadStatusDTO implements Serializable {
    @NotNull(message = "El ID del mensaje es requerido")
    private Long messageId;

    @NotNull(message = "El ID del usuario es requerido")
    private Long userId;

    @NotNull(message = "El ID del chat es requerido")
    private Long chatId;

    private Date timestamp;
}
