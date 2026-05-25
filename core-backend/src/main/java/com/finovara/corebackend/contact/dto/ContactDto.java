package com.finovara.corebackend.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactDto(
        @NotBlank(message = "Name is required")
        @Size(min = 3, max = 30)
        String username,

        @NotBlank(message = "Message is required")
        @Size(max = 800)
        String message,

        @NotBlank(message = "Subject is required")
        @Size(min = 3,max = 50)
        String subject,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 100)
        String email
) {
}
