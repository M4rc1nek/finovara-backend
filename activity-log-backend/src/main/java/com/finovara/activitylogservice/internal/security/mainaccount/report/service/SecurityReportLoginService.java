package com.finovara.activitylogservice.internal.security.report.service;

import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.model.LoginActivity;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.repository.LoginActivityRepository;
import com.finovara.activitylogservice.internal.security.report.dto.ReportLoginDto;
import com.finovara.activitylogservice.internal.security.report.dto.countchart.BrowserCountDto;
import com.finovara.activitylogservice.internal.security.report.dto.countchart.LocationCountDto;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.activity.LoginActivityStatus;
import com.finovara.contracts.percentage.CalculatePercentage;
import com.finovara.contracts.report.dto.ShareStatDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToLongFunction;

@Service
@RequiredArgsConstructor
public class SecurityReportLoginService {

    private final LoginActivityRepository loginActivityRepository;

    public ReportLoginDto getLoginSummary(Long userId, PeriodType periodType) {
        LocalDateTime from = periodType.getStartDate(LocalDate.now()).atStartOfDay();
        LocalDateTime to = LocalDateTime.now();

        long successfulLogins = loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, LoginActivityStatus.SUCCESSFUL, from, to);
        long failedLogins = loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, LoginActivityStatus.UNSUCCESSFUL, from, to);
        long knownDevicesCount = loginActivityRepository.countDistinctDevices(userId, LoginActivityStatus.SUCCESSFUL, from, to);

        LoginActivity firstLogin = findFirstLogin(userId, LoginActivityStatus.SUCCESSFUL, from, to).orElse(null);
        LoginActivity lastLogin = findLastLogin(userId, LoginActivityStatus.SUCCESSFUL, from, to).orElse(null);

        List<LocationCountDto> locationCounts = loginActivityRepository.findLocationCounts(userId, LoginActivityStatus.SUCCESSFUL, from, to);
        List<BrowserCountDto> browserCounts = loginActivityRepository.findBrowserCounts(userId, LoginActivityStatus.SUCCESSFUL, from, to);

        return new ReportLoginDto(
                successfulLogins,
                failedLogins,
                knownDevicesCount,
                extractLocation(firstLogin),
                extractDate(firstLogin),
                extractLocation(lastLogin),
                extractDate(lastLogin),
                locationCounts.size(),
                toShares(locationCounts, LocationCountDto::location, LocationCountDto::count),
                toShares(browserCounts, BrowserCountDto::browser, BrowserCountDto::count)
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

    private <T> List<ShareStatDto> toShares(List<T> entries, Function<T, String> labelExtractor, ToLongFunction<T> countExtractor) {
        long totalCount = entries.stream().mapToLong(countExtractor).sum();

        return entries.stream()
                .map(entry -> new ShareStatDto(
                        labelExtractor.apply(entry),
                        CalculatePercentage.calculatePercentage(BigDecimal.valueOf(countExtractor.applyAsLong(entry)), BigDecimal.valueOf(totalCount))))
                .toList();
    }
}