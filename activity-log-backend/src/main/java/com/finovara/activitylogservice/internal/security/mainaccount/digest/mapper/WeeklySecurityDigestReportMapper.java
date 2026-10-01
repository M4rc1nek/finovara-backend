package com.finovara.activitylogservice.internal.security.digest.mapper;

import com.finovara.activitylogservice.internal.security.digest.dto.AccountChangesDigestSummaryDto;
import com.finovara.activitylogservice.internal.security.digest.dto.LoginDigestSummaryDto;
import com.finovara.contracts.notification.email.digest.report.security.WeeklySecurityDigestReportDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class WeeklySecurityDigestReportMapper {

    public WeeklySecurityDigestReportDto toDto(Long userId, LocalDateTime weekStart, LocalDateTime weekEnd, LoginDigestSummaryDto loginSummary, AccountChangesDigestSummaryDto accountChangesSummary) {
        return new WeeklySecurityDigestReportDto(
                userId,
                weekStart,
                weekEnd,
                loginSummary.successfulLogins(),
                loginSummary.failedLogins(),
                loginSummary.ipAddresses(),
                loginSummary.locations(),
                loginSummary.browsers(),
                accountChangesSummary.passwordChanges(),
                accountChangesSummary.lastPasswordChangeDate(),
                accountChangesSummary.emailChanges(),
                accountChangesSummary.lastEmailChangeDate(),
                accountChangesSummary.usernameChanges(),
                accountChangesSummary.lastUsernameChangeDate(),
                accountChangesSummary.profileImageChanges(),
                accountChangesSummary.lastProfileImageChangeDate(),
                accountChangesSummary.profileImageDeleted(),
                accountChangesSummary.lastProfileImageDeleteDate()
        );
    }
}