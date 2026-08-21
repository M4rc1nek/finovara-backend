package com.finovara.activitylogservice.internal.security.digest;

import com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.activity.repository.AccountChangesActivityRepository;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.repository.LoginActivityRepository;
import com.finovara.activitylogservice.feignclient.AuthBackendClient;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import com.finovara.contracts.model.activity.LoginActivityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InternalLoginSummaryService {

    private static final int DIGEST_PERIOD_DAYS = 6;

    private final LoginActivityRepository loginActivityRepository;
    private final AccountChangesActivityRepository accountChangesActivityRepository;
    private final AuthBackendClient authBackendClient;

    public List<LoginActivitySummaryDto> getSecurityDigestReport() {
        DateRange range = resolveDigestPeriod();

        return authBackendClient.getAllUserIds().stream()
                .map(userId -> buildSummaryForUser(userId, range))
                .toList();
    }

    private DateRange resolveDigestPeriod() {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(DIGEST_PERIOD_DAYS).atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();
        return new DateRange(from, to);
    }

    private LoginActivitySummaryDto buildSummaryForUser(Long userId, DateRange range) {
        long successfulLogins = countLogins(userId, LoginActivityStatus.SUCCESSFUL, range);
        long failedLogins = countLogins(userId, LoginActivityStatus.UNSUCCESSFUL, range);

        List<String> ipAddresses = loginActivityRepository.findDistinctIpAddresses(userId, LoginActivityStatus.SUCCESSFUL, range.from(), range.to());
        List<String> locations = loginActivityRepository.findDistinctLocations(userId, LoginActivityStatus.SUCCESSFUL, range.from(), range.to());
        List<String> browsers = loginActivityRepository.findDistinctBrowsers(userId, LoginActivityStatus.SUCCESSFUL, range.from(), range.to());

        return new LoginActivitySummaryDto(successfulLogins, failedLogins, ipAddresses, locations, browsers);
    }

    private long countLogins(Long userId, LoginActivityStatus status, DateRange range) {
        return loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(
                userId, status, range.from(), range.to());
    }

    private long countPasswordChanges(Long userId, DateRange range) {
        return accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangesActivityType.PASSWORD_CHANGED, range.from, range.to);
    }

    private LocalDateTime getLastPasswordChangeDate(Long userId, DateRange range) {
        return accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangesActivityType.PASSWORD_CHANGED, range.from, range.to);
    }

    private long countEmailChanges(Long userId, DateRange range) {
        return accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangesActivityType.EMAIL_CHANGED, range.from, range.to);
    }

    private long countUsernameChanges(Long userId, DateRange range) {
        return accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangesActivityType.USERNAME_CHANGED, range.from, range.to);
    }

    private record DateRange(LocalDateTime from, LocalDateTime to) {
    }
}