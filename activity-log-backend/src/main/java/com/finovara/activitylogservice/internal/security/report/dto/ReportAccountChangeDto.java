package com.finovara.activitylogservice.internal.security.report.dto;

import java.time.LocalDate;

public record ReportAccountChangeDto(
        long passwordChanges,
        LocalDate lastPasswordChangeDate,
        long emailChanges,
        LocalDate lastEmailChangeDate,
        boolean additionalAuthorizationEnabled
) {
}
