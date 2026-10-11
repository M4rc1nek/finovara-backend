package com.finovara.activitylogservice.internal.security.mainaccount.digest.dto;

import java.time.LocalDateTime;

public record AccountChangeDigestSummaryDto(
        long passwordChanges,
        LocalDateTime lastPasswordChangeDate,
        long emailChanges,
        LocalDateTime lastEmailChangeDate,
        long usernameChanges,
        LocalDateTime lastUsernameChangeDate,
        long profileImageChanges,
        LocalDateTime lastProfileImageChangeDate,
        long profileImageDeleted,
        LocalDateTime lastProfileImageDeleteDate
) {
}