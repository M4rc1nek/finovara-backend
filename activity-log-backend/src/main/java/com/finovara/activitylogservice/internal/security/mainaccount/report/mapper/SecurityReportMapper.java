package com.finovara.activitylogservice.internal.security.mainaccount.report.mapper;

import com.finovara.activitylogservice.internal.security.mainaccount.report.dto.ReportAccountChangeDto;
import com.finovara.activitylogservice.internal.security.mainaccount.report.dto.ReportLoginDto;
import com.finovara.contracts.report.dto.security.SecurityReportDto;
import org.springframework.stereotype.Component;

@Component
public class SecurityReportMapper {

    public SecurityReportDto toDto(Long userId, ReportLoginDto reportLoginDto, ReportAccountChangeDto reportAccountChangeDto) {
        return new SecurityReportDto(
                userId,
                reportLoginDto.successfulLogins(),
                reportLoginDto.failedLogins(),
                reportLoginDto.knownDevicesCount(),
                reportLoginDto.firstLoginFrom(),
                reportLoginDto.firstLoginAt(),
                reportLoginDto.lastLoginFrom(),
                reportLoginDto.lastLoginAt(),
                reportLoginDto.distinctLocationsCount(),
                reportLoginDto.locationShares(),
                reportLoginDto.browserShares(),
                reportAccountChangeDto.passwordChanges(),
                reportAccountChangeDto.lastPasswordChangeDate(),
                reportAccountChangeDto.emailChanges(),
                reportAccountChangeDto.lastEmailChangeDate(),
                reportAccountChangeDto.additionalAuthorizationEnabled()
        );
    }
}
