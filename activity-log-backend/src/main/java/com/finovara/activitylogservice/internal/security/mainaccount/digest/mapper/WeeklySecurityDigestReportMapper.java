package com.finovara.activitylogservice.internal.security.mainaccount.digest.mapper;

import com.finovara.activitylogservice.internal.security.mainaccount.digest.dto.AccountChangeDigestSummaryDto;
import com.finovara.activitylogservice.internal.security.mainaccount.digest.dto.LoginDigestSummaryDto;
import com.finovara.contracts.mainaccount.report.digest.security.WeeklySecurityDigestReportDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class WeeklySecurityDigestReportMapper {

    public WeeklySecurityDigestReportDto toDto(Long userId, LocalDateTime weekStart, LocalDateTime weekEnd, LoginDigestSummaryDto loginSummary, AccountChangeDigestSummaryDto accountChangeDigestSummaryDto) {
        return new WeeklySecurityDigestReportDto(
                userId,
                weekStart,
                weekEnd,
                loginSummary.successfulLogins(),
                loginSummary.failedLogins(),
                loginSummary.ipAddresses(),
                loginSummary.locations(),
                loginSummary.browsers(),
                accountChangeDigestSummaryDto.passwordChanges(),
                accountChangeDigestSummaryDto.lastPasswordChangeDate(),
                accountChangeDigestSummaryDto.emailChanges(),
                accountChangeDigestSummaryDto.lastEmailChangeDate(),
                accountChangeDigestSummaryDto.usernameChanges(),
                accountChangeDigestSummaryDto.lastUsernameChangeDate(),
                accountChangeDigestSummaryDto.profileImageChanges(),
                accountChangeDigestSummaryDto.lastProfileImageChangeDate(),
                accountChangeDigestSummaryDto.profileImageDeleted(),
                accountChangeDigestSummaryDto.lastProfileImageDeleteDate()
        );
    }
}