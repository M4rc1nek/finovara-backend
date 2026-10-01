package com.finovara.activitylogservice.internal.security.mainaccount.report.service;

import com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.activity.model.AccountChangesActivity;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.activity.repository.AccountChangesActivityRepository;
import com.finovara.activitylogservice.internal.security.mainaccount.report.dto.ReportAccountChangeDto;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SecurityReportAccountChangeService {

    private final AccountChangesActivityRepository accountChangesActivityRepository;

    public ReportAccountChangeDto getAccountChangesSummary(Long userId, PeriodType periodType) {
        LocalDateTime from = periodType.getStartDate(LocalDate.now()).atStartOfDay();
        LocalDateTime to = LocalDateTime.now();

        long passwordChanges = accountChangesActivityRepository.countByUserIdAndTypeAndCreatedAtBetween(userId, AccountChangesActivityType.PASSWORD_CHANGED, from, to);
        LocalDate lastPasswordChangeDate = findLastChangeDate(userId, AccountChangesActivityType.PASSWORD_CHANGED, from, to);

        long emailChanges = accountChangesActivityRepository.countByUserIdAndTypeAndCreatedAtBetween(userId, AccountChangesActivityType.EMAIL_CHANGED, from, to);
        LocalDate lastEmailChangeDate = findLastChangeDate(userId, AccountChangesActivityType.EMAIL_CHANGED, from, to);

        return new ReportAccountChangeDto(
                passwordChanges,
                lastPasswordChangeDate,
                emailChanges,
                lastEmailChangeDate,
                isAdditionalAuthorizationEnabled(userId)
        );
    }

    private LocalDate findLastChangeDate(Long userId, AccountChangesActivityType type, LocalDateTime from, LocalDateTime to) {
        return findLastActivity(userId, type, from, to)
                .map(activity -> activity.getCreatedAt().toLocalDate())
                .orElse(null);
    }

    private Optional<AccountChangesActivity> findLastActivity(Long userId, AccountChangesActivityType type, LocalDateTime from, LocalDateTime to) {
        return accountChangesActivityRepository.findActivities(userId, type, from, to, PageRequest.of(0, 1))
                .stream().findFirst();
    }

    private boolean isAdditionalAuthorizationEnabled(Long userId) {
        return findLastAuthorizationStatusChange(userId)
                .map(activity -> activity.getType() == AccountChangesActivityType.ADDITIONAL_AUTHORIZATION_ENABLED)
                .orElse(false);
    }

    private Optional<AccountChangesActivity> findLastAuthorizationStatusChange(Long userId) {
        return accountChangesActivityRepository.findAuthorizationStatusChanges(userId, PageRequest.of(0, 1))
                .stream().findFirst();
    }
}