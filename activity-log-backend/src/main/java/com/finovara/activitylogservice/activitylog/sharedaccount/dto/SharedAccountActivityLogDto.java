package com.finovara.activitylogservice.activitylog.sharedaccount.dto;

import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;

import java.time.LocalDateTime;

public record SharedAccountActivityLogDto(
        Long userId,
        String username,
        String email,
        String profileImagePath,
        SharedAccountActivityLogType type,
        LocalDateTime createdAt
) {
}