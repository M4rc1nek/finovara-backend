package com.finovara.activitylogservice.internal.security.digest.service;

import com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.activity.repository.AccountChangesActivityRepository;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.repository.LoginActivityRepository;
import com.finovara.activitylogservice.internal.security.digest.dto.AccountChangesDigestSummaryDto;
import com.finovara.activitylogservice.internal.security.digest.dto.LoginDigestSummaryDto;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import com.finovara.contracts.model.activity.LoginActivityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SecuritySummaryService {

    private final LoginActivityRepository loginActivityRepository;
    private final AccountChangesActivityRepository accountChangesActivityRepository;

    public LoginDigestSummaryDto calculateLoginSummary(Long userId, LocalDateTime from, LocalDateTime to) {
        long successfulLogins = countLogins(userId, LoginActivityStatus.SUCCESSFUL, from, to);
        long failedLogins = countLogins(userId, LoginActivityStatus.UNSUCCESSFUL, from, to);

        List<String> ipAddresses = loginActivityRepository.findDistinctIpAddresses(userId, LoginActivityStatus.SUCCESSFUL, from, to);
        List<String> locations = loginActivityRepository.findDistinctLocations(userId, LoginActivityStatus.SUCCESSFUL, from, to);
        List<String> browsers = loginActivityRepository.findDistinctBrowsers(userId, LoginActivityStatus.SUCCESSFUL, from, to);

        return new LoginDigestSummaryDto(successfulLogins, failedLogins, ipAddresses, locations, browsers);
    }

    public AccountChangesDigestSummaryDto calculateAccountChangesSummary(Long userId, LocalDateTime from, LocalDateTime to) {
        long passwordChanges = countPasswordChanges(userId, from, to);
        LocalDateTime lastPasswordChangeDate = getLastPasswordChangeDate(userId, from, to);

        long emailChanges = countEmailChanges(userId, from, to);
        LocalDateTime lastEmailChangeDate = getLastEmailChangeDate(userId, from, to);

        long usernameChanges = countUsernameChanges(userId, from, to);
        LocalDateTime lastUsernameChangeDate = getLastUsernameChangeDate(userId, from, to);

        long profileImageChanges = countProfileImageChanges(userId, from, to);
        LocalDateTime lastProfileImageChangeDate = getLastProfileImageChangeDate(userId, from, to);

        long profileImageDeleted = countProfileImageDeleted(userId, from, to);
        LocalDateTime lastProfileImageDeleteDate = getLastProfileImageDeleteDate(userId, from, to);

        return new AccountChangesDigestSummaryDto(passwordChanges, lastPasswordChangeDate, emailChanges, lastEmailChangeDate, usernameChanges, lastUsernameChangeDate, profileImageChanges, lastProfileImageChangeDate, profileImageDeleted, lastProfileImageDeleteDate);
    }

    private long countLogins(Long userId, LoginActivityStatus status, LocalDateTime from, LocalDateTime to) {
        return loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, status, from, to);
    }

    private long countPasswordChanges(Long userId, LocalDateTime from, LocalDateTime to) {
        return accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangesActivityType.PASSWORD_CHANGED, from, to);
    }

    private LocalDateTime getLastPasswordChangeDate(Long userId, LocalDateTime from, LocalDateTime to) {
        return accountChangesActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangesActivityType.PASSWORD_CHANGED, from, to);
    }

    private long countEmailChanges(Long userId, LocalDateTime from, LocalDateTime to) {
        return accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangesActivityType.EMAIL_CHANGED, from, to);
    }

    private LocalDateTime getLastEmailChangeDate(Long userId, LocalDateTime from, LocalDateTime to) {
        return accountChangesActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangesActivityType.EMAIL_CHANGED, from, to);
    }

    private long countUsernameChanges(Long userId, LocalDateTime from, LocalDateTime to) {
        return accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangesActivityType.USERNAME_CHANGED, from, to);
    }

    private LocalDateTime getLastUsernameChangeDate(Long userId, LocalDateTime from, LocalDateTime to) {
        return accountChangesActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangesActivityType.USERNAME_CHANGED, from, to);
    }

    private long countProfileImageChanges(Long userId, LocalDateTime from, LocalDateTime to) {
        return accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangesActivityType.PROFILE_IMG_CHANGED, from, to);
    }

    private LocalDateTime getLastProfileImageChangeDate(Long userId, LocalDateTime from, LocalDateTime to) {
        return accountChangesActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangesActivityType.PROFILE_IMG_CHANGED, from, to);
    }

    private long countProfileImageDeleted(Long userId, LocalDateTime from, LocalDateTime to) {
        return accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangesActivityType.PROFILE_IMG_DELETED, from, to);
    }

    private LocalDateTime getLastProfileImageDeleteDate(Long userId, LocalDateTime from, LocalDateTime to) {
        return accountChangesActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangesActivityType.PROFILE_IMG_DELETED, from, to);
    }
}