package com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.dto;

import com.finovara.contracts.mainaccount.activity.model.AccountChangeActivityType;

import java.time.LocalDateTime;

public record AccountChangeActivityDto(
        AccountChangeActivityType type,
        LocalDateTime createdAt,
        String browser,
        String ipAddress,
        String location
) {
}
