package com.finovara.activitylogservice.internal.security.mainaccount.report.dto;

import com.finovara.contracts.report.dto.security.ShareStatDto;

import java.time.LocalDate;
import java.util.List;


public record ReportLoginDto(
        long successfulLogins,
        long failedLogins,
        long knownDevicesCount,
        String firstLoginFrom,
        LocalDate firstLoginAt,
        String lastLoginFrom,
        LocalDate lastLoginAt,
        int distinctLocationsCount,
        List<ShareStatDto> locationShares,
        List<ShareStatDto> browserShares
) {
}