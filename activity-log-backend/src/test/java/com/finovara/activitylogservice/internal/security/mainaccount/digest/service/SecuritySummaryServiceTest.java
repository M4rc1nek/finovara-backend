package com.finovara.activitylogservice.internal.security.digest.service;

import com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.activity.repository.AccountChangesActivityRepository;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.repository.LoginActivityRepository;
import com.finovara.activitylogservice.internal.security.digest.dto.AccountChangesDigestSummaryDto;
import com.finovara.activitylogservice.internal.security.digest.dto.LoginDigestSummaryDto;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import com.finovara.contracts.model.activity.LoginActivityStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecuritySummaryServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime FROM = LocalDateTime.of(2026, 8, 10, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 8, 17, 0, 0);

    @Mock
    private LoginActivityRepository loginActivityRepository;

    @Mock
    private AccountChangesActivityRepository accountChangesActivityRepository;

    private SecuritySummaryService securitySummaryService;

    @BeforeEach
    void setUp() {
        securitySummaryService = new SecuritySummaryService(loginActivityRepository, accountChangesActivityRepository);
    }

    @Nested
    class CalculateLoginSummary {

        @Test
        void shouldReturnLoginSummaryWhenActivitiesExist() {
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, LoginActivityStatus.SUCCESSFUL, FROM, TO)).thenReturn(10L);
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, LoginActivityStatus.UNSUCCESSFUL, FROM, TO)).thenReturn(3L);
            when(loginActivityRepository.findDistinctIpAddresses(USER_ID, LoginActivityStatus.SUCCESSFUL, FROM, TO)).thenReturn(List.of("1.1.1.1"));
            when(loginActivityRepository.findDistinctLocations(USER_ID, LoginActivityStatus.SUCCESSFUL, FROM, TO)).thenReturn(List.of("Warsaw"));
            when(loginActivityRepository.findDistinctBrowsers(USER_ID, LoginActivityStatus.SUCCESSFUL, FROM, TO)).thenReturn(List.of("Chrome"));

            LoginDigestSummaryDto result = securitySummaryService.calculateLoginSummary(USER_ID, FROM, TO);

            assertEquals(10L, result.successfulLogins());
            assertEquals(3L, result.failedLogins());
            assertEquals(List.of("1.1.1.1"), result.ipAddresses());
            assertEquals(List.of("Warsaw"), result.locations());
            assertEquals(List.of("Chrome"), result.browsers());
        }

        @Test
        void shouldReturnZeroCountsAndEmptyListsWhenNoActivitiesExist() {
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, LoginActivityStatus.SUCCESSFUL, FROM, TO)).thenReturn(0L);
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, LoginActivityStatus.UNSUCCESSFUL, FROM, TO)).thenReturn(0L);
            when(loginActivityRepository.findDistinctIpAddresses(USER_ID, LoginActivityStatus.SUCCESSFUL, FROM, TO)).thenReturn(List.of());
            when(loginActivityRepository.findDistinctLocations(USER_ID, LoginActivityStatus.SUCCESSFUL, FROM, TO)).thenReturn(List.of());
            when(loginActivityRepository.findDistinctBrowsers(USER_ID, LoginActivityStatus.SUCCESSFUL, FROM, TO)).thenReturn(List.of());

            LoginDigestSummaryDto result = securitySummaryService.calculateLoginSummary(USER_ID, FROM, TO);

            assertEquals(0L, result.successfulLogins());
            assertEquals(0L, result.failedLogins());
            assertEquals(List.of(), result.ipAddresses());
            assertEquals(List.of(), result.locations());
            assertEquals(List.of(), result.browsers());
        }

        @Test
        void shouldQueryOnlySuccessfulLoginsWhenFetchingIpsLocationsAndBrowsers() {
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, LoginActivityStatus.SUCCESSFUL, FROM, TO)).thenReturn(1L);
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, LoginActivityStatus.UNSUCCESSFUL, FROM, TO)).thenReturn(1L);
            when(loginActivityRepository.findDistinctIpAddresses(USER_ID, LoginActivityStatus.SUCCESSFUL, FROM, TO)).thenReturn(List.of());
            when(loginActivityRepository.findDistinctLocations(USER_ID, LoginActivityStatus.SUCCESSFUL, FROM, TO)).thenReturn(List.of());
            when(loginActivityRepository.findDistinctBrowsers(USER_ID, LoginActivityStatus.SUCCESSFUL, FROM, TO)).thenReturn(List.of());

            securitySummaryService.calculateLoginSummary(USER_ID, FROM, TO);

            verify(loginActivityRepository).findDistinctIpAddresses(USER_ID, LoginActivityStatus.SUCCESSFUL, FROM, TO);
            verify(loginActivityRepository).findDistinctLocations(USER_ID, LoginActivityStatus.SUCCESSFUL, FROM, TO);
            verify(loginActivityRepository).findDistinctBrowsers(USER_ID, LoginActivityStatus.SUCCESSFUL, FROM, TO);
        }
    }

    @Nested
    class CalculateAccountChangesSummary {

        @Test
        void shouldReturnAccountChangesSummaryWhenAllChangesExist() {
            LocalDateTime passwordDate = LocalDateTime.of(2026, 8, 11, 10, 0);
            LocalDateTime emailDate = LocalDateTime.of(2026, 8, 12, 10, 0);
            LocalDateTime usernameDate = LocalDateTime.of(2026, 8, 13, 10, 0);
            LocalDateTime profileImageDate = LocalDateTime.of(2026, 8, 14, 10, 0);
            LocalDateTime profileImageDeleteDate = LocalDateTime.of(2026, 8, 15, 10, 0);

            when(accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PASSWORD_CHANGED, FROM, TO)).thenReturn(1L);
            when(accountChangesActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PASSWORD_CHANGED, FROM, TO)).thenReturn(passwordDate);
            when(accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.EMAIL_CHANGED, FROM, TO)).thenReturn(2L);
            when(accountChangesActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.EMAIL_CHANGED, FROM, TO)).thenReturn(emailDate);
            when(accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.USERNAME_CHANGED, FROM, TO)).thenReturn(3L);
            when(accountChangesActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.USERNAME_CHANGED, FROM, TO)).thenReturn(usernameDate);
            when(accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PROFILE_IMG_CHANGED, FROM, TO)).thenReturn(4L);
            when(accountChangesActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PROFILE_IMG_CHANGED, FROM, TO)).thenReturn(profileImageDate);
            when(accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PROFILE_IMG_DELETED, FROM, TO)).thenReturn(5L);
            when(accountChangesActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PROFILE_IMG_DELETED, FROM, TO)).thenReturn(profileImageDeleteDate);

            AccountChangesDigestSummaryDto result = securitySummaryService.calculateAccountChangesSummary(USER_ID, FROM, TO);

            assertEquals(1L, result.passwordChanges());
            assertEquals(passwordDate, result.lastPasswordChangeDate());
            assertEquals(2L, result.emailChanges());
            assertEquals(emailDate, result.lastEmailChangeDate());
            assertEquals(3L, result.usernameChanges());
            assertEquals(usernameDate, result.lastUsernameChangeDate());
            assertEquals(4L, result.profileImageChanges());
            assertEquals(profileImageDate, result.lastProfileImageChangeDate());
            assertEquals(5L, result.profileImageDeleted());
            assertEquals(profileImageDeleteDate, result.lastProfileImageDeleteDate());
        }

        @Test
        void shouldReturnZeroCountsAndNullDatesWhenNoChangesExist() {
            when(accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PASSWORD_CHANGED, FROM, TO)).thenReturn(0L);
            when(accountChangesActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PASSWORD_CHANGED, FROM, TO)).thenReturn(null);
            when(accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.EMAIL_CHANGED, FROM, TO)).thenReturn(0L);
            when(accountChangesActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.EMAIL_CHANGED, FROM, TO)).thenReturn(null);
            when(accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.USERNAME_CHANGED, FROM, TO)).thenReturn(0L);
            when(accountChangesActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.USERNAME_CHANGED, FROM, TO)).thenReturn(null);
            when(accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PROFILE_IMG_CHANGED, FROM, TO)).thenReturn(0L);
            when(accountChangesActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PROFILE_IMG_CHANGED, FROM, TO)).thenReturn(null);
            when(accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PROFILE_IMG_DELETED, FROM, TO)).thenReturn(0L);
            when(accountChangesActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PROFILE_IMG_DELETED, FROM, TO)).thenReturn(null);

            AccountChangesDigestSummaryDto result = securitySummaryService.calculateAccountChangesSummary(USER_ID, FROM, TO);

            assertEquals(0L, result.passwordChanges());
            assertNull(result.lastPasswordChangeDate());
            assertEquals(0L, result.emailChanges());
            assertNull(result.lastEmailChangeDate());
            assertEquals(0L, result.usernameChanges());
            assertNull(result.lastUsernameChangeDate());
            assertEquals(0L, result.profileImageChanges());
            assertNull(result.lastProfileImageChangeDate());
            assertEquals(0L, result.profileImageDeleted());
            assertNull(result.lastProfileImageDeleteDate());
        }

        @Test
        void shouldQueryRepositoryForEachChangeTypeWhenCalculatingAccountChangesSummary() {
            when(accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PASSWORD_CHANGED, FROM, TO)).thenReturn(0L);
            when(accountChangesActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PASSWORD_CHANGED, FROM, TO)).thenReturn(null);
            when(accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.EMAIL_CHANGED, FROM, TO)).thenReturn(0L);
            when(accountChangesActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.EMAIL_CHANGED, FROM, TO)).thenReturn(null);
            when(accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.USERNAME_CHANGED, FROM, TO)).thenReturn(0L);
            when(accountChangesActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.USERNAME_CHANGED, FROM, TO)).thenReturn(null);
            when(accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PROFILE_IMG_CHANGED, FROM, TO)).thenReturn(0L);
            when(accountChangesActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PROFILE_IMG_CHANGED, FROM, TO)).thenReturn(null);
            when(accountChangesActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PROFILE_IMG_DELETED, FROM, TO)).thenReturn(0L);
            when(accountChangesActivityRepository.findLastChangeDateByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PROFILE_IMG_DELETED, FROM, TO)).thenReturn(null);

            securitySummaryService.calculateAccountChangesSummary(USER_ID, FROM, TO);

            verify(accountChangesActivityRepository).countByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PASSWORD_CHANGED, FROM, TO);
            verify(accountChangesActivityRepository).countByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.EMAIL_CHANGED, FROM, TO);
            verify(accountChangesActivityRepository).countByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.USERNAME_CHANGED, FROM, TO);
            verify(accountChangesActivityRepository).countByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PROFILE_IMG_CHANGED, FROM, TO);
            verify(accountChangesActivityRepository).countByUserIdAndStatusAndCreatedAtBetween(USER_ID, AccountChangesActivityType.PROFILE_IMG_DELETED, FROM, TO);
        }
    }
}