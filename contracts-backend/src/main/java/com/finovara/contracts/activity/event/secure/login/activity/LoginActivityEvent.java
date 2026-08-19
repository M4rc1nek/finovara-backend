package com.finovara.contracts.activity.event.secure.login.activity;

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