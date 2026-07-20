package com.finovara.financeservice.sharedaccount.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record SharedAccountNoteDto(
        Long id,
        @NotBlank @Size(min = 5, max = 40) String topic,
        @NotBlank @Size(min = 5, max = 600) String description,
        LocalDateTime createdAt,
        Long noteCreatorId,
        String noteCreatorUsername
) {
}