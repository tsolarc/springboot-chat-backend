package com.Backend.Chat.dto.Message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendMessageRequest(
        @NotNull UUID clientMessageId,
        @NotBlank @Size(max = 5000) String content
) {}
