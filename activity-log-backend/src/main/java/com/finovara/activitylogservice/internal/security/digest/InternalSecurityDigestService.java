package com.finovara.activitylogservice.internal.security.digest;

import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.activitylogservice.internal.security.digest.dto.AccountChangesDigestSummaryDto;
import com.finovara.activitylogservice.internal.security.digest.dto.LoginDigestSummaryDto;
import com.finovara.activitylogservice.internal.security.digest.mapper.WeeklySecurityDigestReportMapper;
import com.finovara.activitylogservice.internal.security.digest.service.SecuritySummaryService;
import com.finovara.contracts.notification.email.digest.report.security.WeeklySecurityDigestReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InternalSecurityDigestService {

    private static final int DIGEST_PERIOD_DAYS = 6;

    private final SecuritySummaryService securitySummaryService;
    private final WeeklySecurityDigestReportMapper weeklySecurityDigestReportMapper;
    private final AuthBackendClient authBackendClient;

    public List<WeeklySecurityDigestReportDto> getSecurityDigestReport() {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(DIGEST_PERIOD_DAYS).atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        return authBackendClient.getAllUserIds().stream()
                .map(userId -> buildSummaryForUser(userId, from, to))
                .toList();
    }

    private WeeklySecurityDigestReportDto buildSummaryForUser(Long userId, LocalDateTime from, LocalDateTime to) {
        LoginDigestSummaryDto loginSummary = securitySummaryService.calculateLoginSummary(userId, from, to);
        AccountChangesDigestSummaryDto accountChangesSummary = securitySummaryService.calculateAccountChangesSummary(userId, from, to);

        return weeklySecurityDigestReportMapper.toDto(userId, from, to, loginSummary, accountChangesSummary);
    }
}