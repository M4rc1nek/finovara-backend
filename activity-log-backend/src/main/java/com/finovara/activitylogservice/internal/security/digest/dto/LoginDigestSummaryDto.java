package com.finovara.activitylogservice.internal.security.digest.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SecurityDigestSummaryDto(
        Long userId,
        LocalDate weekStart,
        LocalDate weekEnd,
        long successfulLogins,
        long failedLogins,
        List<String> ipAddresses,
        List<String> locations,
        List<String> browsers,
        long passwordChanges,
        LocalDateTime lastPasswordChangeDate,
        long emailChanges,
        LocalDateTime lastEmailChangeDate,
        long usernameChanges,
        LocalDateTime lastUsernameChangeDate
) {
}
