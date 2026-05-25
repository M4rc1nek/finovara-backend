package com.finovara.activityservice.contracts.event.secure.login.activity;

import com.finovara.activityservice.contracts.model.activity.LoginActivityStatus;

import java.time.LocalDateTime;

public record LoginActivityEvent(
        Long userId,
        LoginActivityStatus status,
        String browser,
        String ipAddress,
        String location,
        LocalDateTime occurredAt
) {}