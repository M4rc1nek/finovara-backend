package com.finovara.activitylogservice.internal.security.mainaccount.report.service;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.model.AccountChangeActivity;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.repository.AccountChangeActivityRepository;
import com.finovara.activitylogservice.internal.security.mainaccount.report.dto.ReportAccountChangeDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.mainaccount.activity.model.AccountChangeActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SecurityReportAccountChangeService {

    private final AccountChangeActivityRepository accountChangeActivityRepository;

    public ReportAccountChangeDto getAccountChangesSummary(Long userId, PeriodType periodType) {
        LocalDateTime from = periodType.getStartDate(LocalDate.now()).atStartOfDay();
        LocalDateTime to = LocalDateTime.now();

        long passwordChanges = accountChangeActivityRepository.countByUserIdAndTypeAndCreatedAtBetween(userId, AccountChangeActivityType.PASSWORD_CHANGED, from, to);
        LocalDate lastPasswordChangeDate = findLastChangeDate(userId, AccountChangeActivityType.PASSWORD_CHANGED, from, to);

        long emailChanges = accountChangeActivityRepository.countByUserIdAndTypeAndCreatedAtBetween(userId, AccountChangeActivityType.EMAIL_CHANGED, from, to);
        LocalDate lastEmailChangeDate = findLastChangeDate(userId, AccountChangeActivityType.EMAIL_CHANGED, from, to);

        return new ReportAccountChangeDto(
                passwordChanges,
                lastPasswordChangeDate,
                emailChanges,
                lastEmailChangeDate,
                isAdditionalAuthorizationEnabled(userId)
        );
    }

    private LocalDate findLastChangeDate(Long userId, AccountChangeActivityType type, LocalDateTime from, LocalDateTime to) {
        return findLastActivity(userId, type, from, to)
                .map(activity -> activity.getCreatedAt().toLocalDate())
                .orElse(null);
    }

    private Optional<AccountChangeActivity> findLastActivity(Long userId, AccountChangeActivityType type, LocalDateTime from, LocalDateTime to) {
        return accountChangeActivityRepository.findActivitiesByUserId(userId, type, from, to, PageRequest.of(0, 1))
                .stream().findFirst();
    }

    private boolean isAdditionalAuthorizationEnabled(Long userId) {
        return findLastAuthorizationStatusChange(userId)
                .map(activity -> activity.getType() == AccountChangeActivityType.ADDITIONAL_AUTHORIZATION_ENABLED)
                .orElse(false);
    }

    private Optional<AccountChangeActivity> findLastAuthorizationStatusChange(Long userId) {
        return accountChangeActivityRepository.findAuthorizationStatusChanges(userId, PageRequest.of(0, 1))
                .stream().findFirst();
    }
}