package com.finovara.contracts.event.activity.secure.login.activity;

import com.finovara.contracts.model.activity.LoginActivityStatus;

import java.time.LocalDateTime;

public record LoginActivityEvent(
        Long userId,
        LoginActivityStatus status,
        String browser,
        String ipAddress,
        String location,
        LocalDateTime occurredAt
) {}