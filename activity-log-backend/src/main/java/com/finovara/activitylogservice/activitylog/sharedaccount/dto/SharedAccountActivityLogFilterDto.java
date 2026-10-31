package com.finovara.activitylogservice.activitylog.sharedaccount.dto;

import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import com.finovara.contracts.sharedaccount.SharedRole;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record SharedAccountActivityLogFilterDto(
        SharedAccountActivityLogType type,
        Long targetId,
        Long userId,
        SharedRole role,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAt
) {
}