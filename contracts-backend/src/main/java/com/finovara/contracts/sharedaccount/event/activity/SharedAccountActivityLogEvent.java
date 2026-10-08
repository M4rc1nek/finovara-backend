package com.finovara.contracts.sharedaccount.event.activity;

import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;

import java.time.LocalDateTime;

public record SharedAccountActivityLogEvent(
        Long ownerId,
        Long memberId,
        Long userId,
        Long targetId,
        SharedAccountActivityLogType type,
        LocalDateTime createdAt) {

}