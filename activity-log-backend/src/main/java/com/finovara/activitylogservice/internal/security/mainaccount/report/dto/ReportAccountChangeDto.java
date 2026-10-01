package com.finovara.activitylogservice.internal.security.mainaccount.report.dto;

import java.time.LocalDate;

public record ReportAccountChangeDto(
        long passwordChanges,
        LocalDate lastPasswordChangeDate,
        long emailChanges,
        LocalDate lastEmailChangeDate,
        boolean additionalAuthorizationEnabled
) {
}
