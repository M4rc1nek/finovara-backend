package com.finovara.contracts.report.dto;

import java.time.LocalDate;
import java.util.List;

public record SecurityReportDto(
        Long userId,
        long successfulLogins,
        long failedLogins,
        long knownDevicesCount,
        String firstLoginFrom,
        LocalDate firstLoginAt,
        String lastLoginFrom,
        LocalDate lastLoginAt,
        int distinctLocationsCount,
        List<ShareStatDto> locationShares,
        List<ShareStatDto> browserShares,
        long passwordChanges,
        LocalDate lastPasswordChangeDate,
        long emailChanges,
        LocalDate lastEmailChangeDate,
        boolean additionalAuthorizationEnabled
) {
}
