package com.Backend.Chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TypingDTO implements Serializable {
    @NotNull(message = "El ID del usuario es requerido")
    private Long userId;

    @NotBlank(message = "El nombre de usuario es requerido")
    @Size(max = 50, message = "El nombre de usuario no puede exceder 50 caracteres")
    private String username;

    @NotNull(message = "El ID del chat es requerido")
    private Long chatId;

    private Date timestamp;

    private boolean isTyping = true;
}
