package com.finovara.contracts.mainaccount.activity.event.secure.login.activity;

import com.finovara.contracts.mainaccount.activity.model.LoginActivityStatus;

import java.time.LocalDateTime;

public record LoginActivityEvent(
        Long userId,
        LoginActivityStatus status,
        String browser,
        String ipAddress,
        String location,
        LocalDateTime occurredAt
) {}