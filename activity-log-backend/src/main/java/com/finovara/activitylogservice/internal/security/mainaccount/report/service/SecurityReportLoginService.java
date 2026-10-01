package com.finovara.activitylogservice.internal.security.mainaccount.report.service;

import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.model.LoginActivity;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.repository.LoginActivityRepository;
import com.finovara.activitylogservice.internal.security.mainaccount.report.dto.ReportLoginDto;
import com.finovara.activitylogservice.internal.security.util.clientinfo.ClientInfoResolver;
import com.finovara.activitylogservice.internal.security.util.clientinfo.dto.ClientInfoDto;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.activity.LoginActivityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SecurityReportLoginService {

    private final LoginActivityRepository loginActivityRepository;
    private final ClientInfoResolver clientInfoResolver;

    public ReportLoginDto getLoginSummary(Long userId, PeriodType periodType) {
        LocalDateTime from = periodType.getStartDate(LocalDate.now()).atStartOfDay();
        LocalDateTime to = LocalDateTime.now();

        long successfulLogins = loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, LoginActivityStatus.SUCCESSFUL, from, to);
        long failedLogins = loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, LoginActivityStatus.UNSUCCESSFUL, from, to);
        long knownDevicesCount = loginActivityRepository.countDistinctDevices(userId, LoginActivityStatus.SUCCESSFUL, from, to);

        LoginActivity firstLogin = findFirstLogin(userId, LoginActivityStatus.SUCCESSFUL, from, to).orElse(null);
        LoginActivity lastLogin = findLastLogin(userId, LoginActivityStatus.SUCCESSFUL, from, to).orElse(null);

        ClientInfoDto clientInfoDto = clientInfoResolver.getClientContext(userId, from, to);

        return new ReportLoginDto(
                successfulLogins,
                failedLogins,
                knownDevicesCount,
                extractLocation(firstLogin),
                extractDate(firstLogin),
                extractLocation(lastLogin),
                extractDate(lastLogin),
                clientInfoDto.locationShares().size(),
                clientInfoDto.locationShares(),
                clientInfoDto.browserShares()
        );
    }

    private Optional<LoginActivity> findFirstLogin(Long userId, LoginActivityStatus status, LocalDateTime from, LocalDateTime to) {
        return loginActivityRepository.findFirstLogins(userId, status, from, to, PageRequest.of(0, 1))
                .stream().findFirst();
    }

    private Optional<LoginActivity> findLastLogin(Long userId, LoginActivityStatus status, LocalDateTime from, LocalDateTime to) {
        return loginActivityRepository.findLastLogins(userId, status, from, to, PageRequest.of(0, 1))
                .stream().findFirst();
    }

    private String extractLocation(LoginActivity loginActivity) {
        return loginActivity != null ? loginActivity.getLocation() : null;
    }

    private LocalDate extractDate(LoginActivity loginActivity) {
        return loginActivity != null ? loginActivity.getCreatedAt().toLocalDate() : null;
    }
}