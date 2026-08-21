package com.finovara.authservice.internal.digest.report.email.mapper;

import com.finovara.contracts.notification.email.digest.report.security.WeeklySecurityDigestReportDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class WeeklySecurityDigestReportMapper {

    public WeeklySecurityDigestReportDto toDto(Long userId, LocalDate weekStart, LocalDate weekEnd, long successfulLogins,
                                               long failedLogins, List<String> ipAddresses, List<String> locations, List<String> browsers,
                                               long passwordChanges, LocalDateTime lastPasswordChangeDate, long emailChanges, LocalDateTime
                                                       lastEmailChangeDate, long usernameChanges, LocalDateTime lastUsernameChangeDate
    ) {
        return new WeeklySecurityDigestReportDto(
                userId,
                weekStart,
                weekEnd,
                successfulLogins,
                failedLogins,
                ipAddresses,
                locations,
                browsers,
                passwordChanges,
                lastPasswordChangeDate,
                emailChanges,
                lastEmailChangeDate,
                usernameChanges,
                lastUsernameChangeDate
        );
    }
}