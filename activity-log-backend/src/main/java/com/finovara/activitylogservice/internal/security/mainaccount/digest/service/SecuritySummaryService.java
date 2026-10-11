package com.finovara.activitylogservice.internal.security.mainaccount.digest.service;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.repository.AccountChangeActivityRepository;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.repository.LoginActivityRepository;
import com.finovara.activitylogservice.internal.security.mainaccount.digest.dto.AccountChangeDigestSummaryDto;
import com.finovara.activitylogservice.internal.security.mainaccount.digest.dto.LoginDigestSummaryDto;
import com.finovara.contracts.mainaccount.activity.model.AccountChangeActivityType;
import com.finovara.contracts.mainaccount.activity.model.LoginActivityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SecuritySummaryService {

    private final LoginActivityRepository loginActivityRepository;
    private final AccountChangeActivityRepository accountChangeActivityRepository;

    public LoginDigestSummaryDto calculateLoginSummary(Long userId, LocalDateTime from, LocalDateTime to) {
        long successfulLogins = countLogins(userId, LoginActivityStatus.SUCCESSFUL, from, to);
        long failedLogins = countLogins(userId, LoginActivityStatus.UNSUCCESSFUL, from, to);

        List<String> ipAddresses = loginActivityRepository.findDistinctIpAddresses(userId, LoginActivityStatus.SUCCESSFUL, from, to);
        List<String> locations = loginActivityRepository.findDistinctLocations(userId, LoginActivityStatus.SUCCESSFUL, from, to);
        List<String> browsers = loginActivityRepository.findDistinctBrowsers(userId, LoginActivityStatus.SUCCESSFUL, from, to);

        return new LoginDigestSummaryDto(successfulLogins, failedLogins, ipAddresses, locations, browsers);
    }

    public AccountChangeDigestSummaryDto calculateAccountChangesSummary(Long userId, LocalDateTime from, LocalDateTime to) {
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

        return new AccountChangeDigestSummaryDto(passwordChanges, lastPasswordChangeDate, emailChanges, lastEmailChangeDate, usernameChanges, lastUsernameChangeDate, profileImageChanges, lastProfileImageChangeDate, profileImageDeleted, lastProfileImageDeleteDate);
    }

    private long countLogins(Long userId, LoginActivityStatus status, LocalDateTime from, LocalDateTime to) {
        return loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, status, from, to);
    }

    private long countPasswordChanges(Long userId, LocalDateTime from, LocalDateTime to) {
        return accountChangeActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangeActivityType.PASSWORD_CHANGED, from, to);
    }

    private LocalDateTime getLastPasswordChangeDate(Long userId, LocalDateTime from, LocalDateTime to) {
        return accountChangeActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangeActivityType.PASSWORD_CHANGED, from, to);
    }

    private long countEmailChanges(Long userId, LocalDateTime from, LocalDateTime to) {
        return accountChangeActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangeActivityType.EMAIL_CHANGED, from, to);
    }

    private LocalDateTime getLastEmailChangeDate(Long userId, LocalDateTime from, LocalDateTime to) {
        return accountChangeActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangeActivityType.EMAIL_CHANGED, from, to);
    }

    private long countUsernameChanges(Long userId, LocalDateTime from, LocalDateTime to) {
        return accountChangeActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangeActivityType.USERNAME_CHANGED, from, to);
    }

    private LocalDateTime getLastUsernameChangeDate(Long userId, LocalDateTime from, LocalDateTime to) {
        return accountChangeActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangeActivityType.USERNAME_CHANGED, from, to);
    }

    private long countProfileImageChanges(Long userId, LocalDateTime from, LocalDateTime to) {
        return accountChangeActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangeActivityType.PROFILE_IMG_CHANGED, from, to);
    }

    private LocalDateTime getLastProfileImageChangeDate(Long userId, LocalDateTime from, LocalDateTime to) {
        return accountChangeActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangeActivityType.PROFILE_IMG_CHANGED, from, to);
    }

    private long countProfileImageDeleted(Long userId, LocalDateTime from, LocalDateTime to) {
        return accountChangeActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangeActivityType.PROFILE_IMG_DELETED, from, to);
    }

    private LocalDateTime getLastProfileImageDeleteDate(Long userId, LocalDateTime from, LocalDateTime to) {
        return accountChangeActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(userId, AccountChangeActivityType.PROFILE_IMG_DELETED, from, to);
    }
}