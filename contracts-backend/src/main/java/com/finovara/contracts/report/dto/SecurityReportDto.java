package com.finovara.activitylogservice.internal.security.report.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SecurityReportDto(
        Long userId,
        long successfulLogins,
        long failedLogins,
        List<String> locations,
        List<String> browsers,
        long passwordChanges,
        LocalDateTime lastPasswordChangeDate,
        long emailChanges,
        LocalDateTime lastEmailChangeDate
) {
}
