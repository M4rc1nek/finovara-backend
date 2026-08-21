package com.finovara.contracts.notification.email.digest.report.security;

import java.time.LocalDateTime;
import java.util.List;

public record WeeklySecurityDigestReportDto(
        Long userId,
        LocalDateTime weekStart,
        LocalDateTime weekEnd,
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
        LocalDateTime lastUsernameChangeDate,
        long profileImageChanges,
        LocalDateTime lastProfileImageChangeDate,
        long profileImageDeleted,
        LocalDateTime lastProfileImageDeleteDate
) {
}