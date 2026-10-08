package com.finovara.contracts.sharedaccount.event.activity;

import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;

import java.time.LocalDateTime;

public record SharedAccountActivityEvent(
        Long ownerId,
        Long memberId,
        Long userId,
        SharedAccountActivityLogType type,
        Long targetId,
        LocalDateTime createdAt) {

}