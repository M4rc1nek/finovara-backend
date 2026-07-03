package com.finovara.authservice.sharedaccount.dto;

import com.finovara.authservice.sharedaccount.model.role.SharedRole;

import java.time.LocalDateTime;

public record SharedAccountMemberDto (
        Long userId,
        String username,
        String profileImagePath,
        SharedRole role,
        LocalDateTime joinedAt
) {}
