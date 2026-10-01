package com.finovara.activitylogservice.internal.security.report.service;

import com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.activity.model.AccountChangesActivity;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.accountchange.activity.repository.AccountChangesActivityRepository;
import com.finovara.activitylogservice.internal.security.report.dto.ReportAccountChangeDto;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.activity.AccountChangesActivityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityReportAccountChangeServiceTest {

    private static final Long USER_ID = 1L;
    private static final PageRequest FIRST_ELEMENT = PageRequest.of(0, 1);
    private static final LocalDateTime PASSWORD_CHANGED_AT = LocalDateTime.of(2024, 5, 10, 12, 30);
    private static final LocalDateTime EMAIL_CHANGED_AT = LocalDateTime.of(2024, 6, 1, 8, 0);

    @Mock
    private AccountChangesActivityRepository accountChangesActivityRepository;

    @Mock
    private AccountChangesActivity passwordActivity;

    @Mock
    private AccountChangesActivity emailActivity;

    @Mock
    private AccountChangesActivity authorizationActivity;

    @Captor
    private ArgumentCaptor<LocalDateTime> fromCaptor;

    @Captor
    private ArgumentCaptor<LocalDateTime> toCaptor;

    @InjectMocks
    private SecurityReportAccountChangeService securityReportAccountChangeService;

    private PeriodType periodType;

    @BeforeEach
    void setUp() {
        periodType = PeriodType.values()[0];
    }

    @Nested
    class PasswordChanges {

        @Test
        void shouldReturnPasswordChangesCountWhenChangesExist() {
            when(accountChangesActivityRepository.countByUserIdAndTypeAndCreatedAtBetween(eq(USER_ID), eq(AccountChangesActivityType.PASSWORD_CHANGED), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(3L);

            ReportAccountChangeDto result = securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType);

            assertEquals(3L, result.passwordChanges());
        }

        @Test
        void shouldReturnZeroPasswordChangesWhenNoChangesExist() {
            ReportAccountChangeDto result = securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType);

            assertEquals(0L, result.passwordChanges());
        }

        @Test
        void shouldReturnLastPasswordChangeDateWhenActivityExists() {
            when(passwordActivity.getCreatedAt()).thenReturn(PASSWORD_CHANGED_AT);
            when(accountChangesActivityRepository.findActivities(eq(USER_ID), eq(AccountChangesActivityType.PASSWORD_CHANGED), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of(passwordActivity));

            ReportAccountChangeDto result = securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType);

            assertEquals(PASSWORD_CHANGED_AT.toLocalDate(), result.lastPasswordChangeDate());
        }

        @Test
        void shouldReturnNullLastPasswordChangeDateWhenNoActivityExists() {
            ReportAccountChangeDto result = securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType);

            assertNull(result.lastPasswordChangeDate());
        }
    }

    @Nested
    class EmailChanges {

        @Test
        void shouldReturnEmailChangesCountWhenChangesExist() {
            when(accountChangesActivityRepository.countByUserIdAndTypeAndCreatedAtBetween(eq(USER_ID), eq(AccountChangesActivityType.PASSWORD_CHANGED), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
            when(accountChangesActivityRepository.countByUserIdAndTypeAndCreatedAtBetween(eq(USER_ID), eq(AccountChangesActivityType.EMAIL_CHANGED), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(2L);

            ReportAccountChangeDto result = securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType);

            assertEquals(2L, result.emailChanges());
        }

        @Test
        void shouldReturnZeroEmailChangesWhenNoChangesExist() {
            ReportAccountChangeDto result = securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType);

            assertEquals(0L, result.emailChanges());
        }

        @Test
        void shouldReturnLastEmailChangeDateWhenActivityExists() {
            when(emailActivity.getCreatedAt()).thenReturn(EMAIL_CHANGED_AT);
            when(accountChangesActivityRepository.findActivities(eq(USER_ID), eq(AccountChangesActivityType.PASSWORD_CHANGED), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of());
            when(accountChangesActivityRepository.findActivities(eq(USER_ID), eq(AccountChangesActivityType.EMAIL_CHANGED), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of(emailActivity));

            ReportAccountChangeDto result = securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType);

            assertEquals(EMAIL_CHANGED_AT.toLocalDate(), result.lastEmailChangeDate());
        }

        @Test
        void shouldReturnNullLastEmailChangeDateWhenNoActivityExists() {
            ReportAccountChangeDto result = securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType);

            assertNull(result.lastEmailChangeDate());
        }

        @Test
        void shouldKeepPasswordAndEmailDatesIndependentWhenOnlyEmailActivityExists() {
            when(emailActivity.getCreatedAt()).thenReturn(EMAIL_CHANGED_AT);
            when(accountChangesActivityRepository.findActivities(eq(USER_ID), eq(AccountChangesActivityType.PASSWORD_CHANGED), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of());
            when(accountChangesActivityRepository.findActivities(eq(USER_ID), eq(AccountChangesActivityType.EMAIL_CHANGED), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of(emailActivity));

            ReportAccountChangeDto result = securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType);

            assertNull(result.lastPasswordChangeDate());
            assertEquals(EMAIL_CHANGED_AT.toLocalDate(), result.lastEmailChangeDate());
        }
    }

    @Nested
    class AdditionalAuthorization {

        @Test
        void shouldReturnTrueWhenLastStatusChangeEnabledAuthorization() {
            when(authorizationActivity.getType()).thenReturn(AccountChangesActivityType.ADDITIONAL_AUTHORIZATION_ENABLED);
            when(accountChangesActivityRepository.findAuthorizationStatusChanges(USER_ID, FIRST_ELEMENT)).thenReturn(List.of(authorizationActivity));

            ReportAccountChangeDto result = securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType);

            assertTrue(result.additionalAuthorizationEnabled());
        }

        @Test
        void shouldReturnFalseWhenLastStatusChangeIsNotEnabling() {
            when(authorizationActivity.getType()).thenReturn(AccountChangesActivityType.PASSWORD_CHANGED);
            when(accountChangesActivityRepository.findAuthorizationStatusChanges(USER_ID, FIRST_ELEMENT)).thenReturn(List.of(authorizationActivity));

            ReportAccountChangeDto result = securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType);

            assertFalse(result.additionalAuthorizationEnabled());
        }

        @Test
        void shouldReturnFalseWhenNoStatusChangeExists() {
            ReportAccountChangeDto result = securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType);

            assertFalse(result.additionalAuthorizationEnabled());
        }
    }

    @Nested
    class DateRange {

        @Test
        void shouldQueryFromStartOfPeriodWhenSummaryIsRequested() {
            LocalDateTime expectedFrom = periodType.getStartDate(LocalDate.now()).atStartOfDay();

            securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType);

            verify(accountChangesActivityRepository, times(2)).countByUserIdAndTypeAndCreatedAtBetween(eq(USER_ID), any(AccountChangesActivityType.class), fromCaptor.capture(), toCaptor.capture());
            assertTrue(fromCaptor.getAllValues().stream().allMatch(from -> from.equals(expectedFrom)));
        }

        @Test
        void shouldQueryUntilCurrentMomentWhenSummaryIsRequested() {
            LocalDateTime before = LocalDateTime.now();

            securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType);

            LocalDateTime after = LocalDateTime.now();
            verify(accountChangesActivityRepository, times(2)).countByUserIdAndTypeAndCreatedAtBetween(eq(USER_ID), any(AccountChangesActivityType.class), fromCaptor.capture(), toCaptor.capture());
            assertTrue(toCaptor.getAllValues().stream().allMatch(to -> !to.isBefore(before) && !to.isAfter(after)));
        }

        @Test
        void shouldUseSameRangeForActivitiesAndCountsWhenSummaryIsRequested() {
            securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType);

            verify(accountChangesActivityRepository).findActivities(eq(USER_ID), eq(AccountChangesActivityType.PASSWORD_CHANGED), fromCaptor.capture(), toCaptor.capture(), eq(FIRST_ELEMENT));
            verify(accountChangesActivityRepository).findActivities(eq(USER_ID), eq(AccountChangesActivityType.EMAIL_CHANGED), fromCaptor.capture(), toCaptor.capture(), eq(FIRST_ELEMENT));
            assertEquals(2, fromCaptor.getAllValues().size());
        }
    }

    @Nested
    class GetAccountChangesSummary {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnSummaryWhenPeriodTypeIsAnySupportedValue(PeriodType type) {
            ReportAccountChangeDto result = securityReportAccountChangeService.getAccountChangesSummary(USER_ID, type);

            assertNotNull(result);
        }

        @Test
        void shouldReturnCompleteSummaryWhenAllActivitiesExist() {
            when(accountChangesActivityRepository.countByUserIdAndTypeAndCreatedAtBetween(eq(USER_ID), eq(AccountChangesActivityType.PASSWORD_CHANGED), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(3L);
            when(accountChangesActivityRepository.countByUserIdAndTypeAndCreatedAtBetween(eq(USER_ID), eq(AccountChangesActivityType.EMAIL_CHANGED), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1L);
            when(passwordActivity.getCreatedAt()).thenReturn(PASSWORD_CHANGED_AT);
            when(emailActivity.getCreatedAt()).thenReturn(EMAIL_CHANGED_AT);
            when(authorizationActivity.getType()).thenReturn(AccountChangesActivityType.ADDITIONAL_AUTHORIZATION_ENABLED);
            when(accountChangesActivityRepository.findActivities(eq(USER_ID), eq(AccountChangesActivityType.PASSWORD_CHANGED), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of(passwordActivity));
            when(accountChangesActivityRepository.findActivities(eq(USER_ID), eq(AccountChangesActivityType.EMAIL_CHANGED), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of(emailActivity));
            when(accountChangesActivityRepository.findAuthorizationStatusChanges(USER_ID, FIRST_ELEMENT)).thenReturn(List.of(authorizationActivity));
            ReportAccountChangeDto expected = new ReportAccountChangeDto(3L, PASSWORD_CHANGED_AT.toLocalDate(), 1L, EMAIL_CHANGED_AT.toLocalDate(), true);

            ReportAccountChangeDto result = securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType);

            assertEquals(expected, result);
        }

        @Test
        void shouldReturnEmptySummaryWhenNoActivitiesExist() {
            ReportAccountChangeDto expected = new ReportAccountChangeDto(0L, null, 0L, null, false);

            ReportAccountChangeDto result = securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType);

            assertEquals(expected, result);
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            assertThrows(NullPointerException.class, () -> securityReportAccountChangeService.getAccountChangesSummary(USER_ID, null));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(accountChangesActivityRepository.countByUserIdAndTypeAndCreatedAtBetween(eq(USER_ID), eq(AccountChangesActivityType.PASSWORD_CHANGED), any(LocalDateTime.class), any(LocalDateTime.class))).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType));
        }
    }
}