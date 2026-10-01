package com.finovara.activitylogservice.internal.security.mainaccount.report.service;

import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.model.LoginActivity;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.repository.LoginActivityRepository;
import com.finovara.activitylogservice.internal.security.mainaccount.report.dto.ReportLoginDto;
import com.finovara.activitylogservice.internal.security.util.clientinfo.dto.BrowserCountDto;
import com.finovara.activitylogservice.internal.security.util.clientinfo.dto.LocationCountDto;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.activity.LoginActivityStatus;
import com.finovara.contracts.percentage.CalculatePercentage;
import com.finovara.contracts.report.dto.security.ShareStatDto;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityReportLoginServiceTest {

    private static final Long USER_ID = 1L;
    private static final PageRequest FIRST_ELEMENT = PageRequest.of(0, 1);
    private static final LocalDateTime FIRST_LOGIN_AT = LocalDateTime.of(2024, 1, 10, 9, 15);
    private static final LocalDateTime LAST_LOGIN_AT = LocalDateTime.of(2024, 6, 20, 18, 45);

    @Mock
    private LoginActivityRepository loginActivityRepository;

    @Mock
    private LoginActivity firstLogin;

    @Mock
    private LoginActivity lastLogin;

    @Captor
    private ArgumentCaptor<LocalDateTime> fromCaptor;

    @Captor
    private ArgumentCaptor<LocalDateTime> toCaptor;

    @InjectMocks
    private SecurityReportLoginService securityReportLoginService;

    private PeriodType periodType;

    @BeforeEach
    void setUp() {
        periodType = PeriodType.values()[0];
    }

    @Nested
    class LoginCounts {

        @Test
        void shouldReturnSuccessfulLoginsCountWhenLoginsExist() {
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(10L);

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(10L, result.successfulLogins());
        }

        @Test
        void shouldReturnZeroCountsWhenNoLoginsExist() {
            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(0L, result.successfulLogins());
            assertEquals(0L, result.failedLogins());
        }

        @Test
        void shouldReturnKnownDevicesCountWhenDevicesExist() {
            when(loginActivityRepository.countDistinctDevices(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(3L);

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(3L, result.knownDevicesCount());
        }
    }

    @Nested
    class FirstAndLastLogin {

        @Test
        void shouldReturnFirstLoginLocationAndDateWhenFirstLoginExists() {
            when(firstLogin.getLocation()).thenReturn("Warsaw");
            when(firstLogin.getCreatedAt()).thenReturn(FIRST_LOGIN_AT);
            when(loginActivityRepository.findFirstLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of(firstLogin));

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals("Warsaw", result.firstLoginFrom());
            assertEquals(FIRST_LOGIN_AT.toLocalDate(), result.firstLoginAt());
        }

        @Test
        void shouldReturnNullFirstLoginDataWhenNoFirstLoginExists() {
            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertNull(result.firstLoginFrom());
            assertNull(result.firstLoginAt());
        }

        @Test
        void shouldReturnLastLoginLocationAndDateWhenLastLoginExists() {
            when(lastLogin.getLocation()).thenReturn("Krakow");
            when(lastLogin.getCreatedAt()).thenReturn(LAST_LOGIN_AT);
            when(loginActivityRepository.findLastLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of(lastLogin));

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals("Krakow", result.lastLoginFrom());
            assertEquals(LAST_LOGIN_AT.toLocalDate(), result.lastLoginAt());
        }

        @Test
        void shouldReturnNullLastLoginDataWhenNoLastLoginExists() {
            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertNull(result.lastLoginFrom());
            assertNull(result.lastLoginAt());
        }

        @Test
        void shouldReturnNullLocationWhenLoginHasNoLocation() {
            when(firstLogin.getLocation()).thenReturn(null);
            when(firstLogin.getCreatedAt()).thenReturn(FIRST_LOGIN_AT);
            when(loginActivityRepository.findFirstLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of(firstLogin));

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertNull(result.firstLoginFrom());
            assertEquals(FIRST_LOGIN_AT.toLocalDate(), result.firstLoginAt());
        }

        @Test
        void shouldKeepFirstAndLastLoginIndependentWhenOnlyLastLoginExists() {
            when(lastLogin.getLocation()).thenReturn("Krakow");
            when(lastLogin.getCreatedAt()).thenReturn(LAST_LOGIN_AT);
            when(loginActivityRepository.findLastLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of(lastLogin));

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertNull(result.firstLoginFrom());
            assertEquals("Krakow", result.lastLoginFrom());
        }
    }

    @Nested
    class LocationShares {

        @Test
        void shouldReturnDistinctLocationsCountWhenLocationsExist() {
            when(loginActivityRepository.findLocationCounts(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(new LocationCountDto("Warsaw", 3L), new LocationCountDto("Krakow", 1L)));

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(2, result.distinctLocationsCount());
        }

        @Test
        void shouldReturnEmptySharesAndZeroDistinctLocationsWhenNoLocationsExist() {
            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(0, result.distinctLocationsCount());
            assertTrue(result.locationShares().isEmpty());
        }

        @Test
        void shouldCalculateLocationSharesWhenMultipleLocationsExist() {
            when(loginActivityRepository.findLocationCounts(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(new LocationCountDto("Warsaw", 3L), new LocationCountDto("Krakow", 1L)));
            List<ShareStatDto> expected = List.of(
                    new ShareStatDto("Warsaw", CalculatePercentage.calculatePercentage(BigDecimal.valueOf(3L), BigDecimal.valueOf(4L))),
                    new ShareStatDto("Krakow", CalculatePercentage.calculatePercentage(BigDecimal.valueOf(1L), BigDecimal.valueOf(4L)))
            );

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(expected, result.locationShares());
        }

        @Test
        void shouldCalculateFullShareWhenSingleLocationExists() {
            when(loginActivityRepository.findLocationCounts(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(new LocationCountDto("Warsaw", 5L)));
            List<ShareStatDto> expected = List.of(
                    new ShareStatDto("Warsaw", CalculatePercentage.calculatePercentage(BigDecimal.valueOf(5L), BigDecimal.valueOf(5L)))
            );

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(expected, result.locationShares());
        }
    }

    @Nested
    class BrowserShares {

        @Test
        void shouldCalculateBrowserSharesWhenMultipleBrowsersExist() {
            when(loginActivityRepository.findBrowserCounts(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(new BrowserCountDto("Chrome", 6L), new BrowserCountDto("Firefox", 2L)));
            List<ShareStatDto> expected = List.of(
                    new ShareStatDto("Chrome", CalculatePercentage.calculatePercentage(BigDecimal.valueOf(6L), BigDecimal.valueOf(8L))),
                    new ShareStatDto("Firefox", CalculatePercentage.calculatePercentage(BigDecimal.valueOf(2L), BigDecimal.valueOf(8L)))
            );

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(expected, result.browserShares());
        }

        @Test
        void shouldReturnEmptyBrowserSharesWhenNoBrowsersExist() {
            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertTrue(result.browserShares().isEmpty());
        }

        @Test
        void shouldCalculateFullShareWhenSingleBrowserExists() {
            when(loginActivityRepository.findBrowserCounts(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(new BrowserCountDto("Chrome", 7L)));
            List<ShareStatDto> expected = List.of(
                    new ShareStatDto("Chrome", CalculatePercentage.calculatePercentage(BigDecimal.valueOf(7L), BigDecimal.valueOf(7L)))
            );

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(expected, result.browserShares());
        }

        @Test
        void shouldNotAffectLocationSharesWhenOnlyBrowsersExist() {
            when(loginActivityRepository.findBrowserCounts(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(new BrowserCountDto("Chrome", 7L)));

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertTrue(result.locationShares().isEmpty());
            assertEquals(0, result.distinctLocationsCount());
        }
    }

    @Nested
    class DateRange {

        @Test
        void shouldQueryFromStartOfPeriodWhenSummaryIsRequested() {
            LocalDateTime expectedFrom = periodType.getStartDate(LocalDate.now()).atStartOfDay();

            securityReportLoginService.getLoginSummary(USER_ID, periodType);

            verify(loginActivityRepository).findLocationCounts(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), fromCaptor.capture(), toCaptor.capture());
            assertEquals(expectedFrom, fromCaptor.getValue());
        }

        @Test
        void shouldQueryUntilCurrentMomentWhenSummaryIsRequested() {
            LocalDateTime before = LocalDateTime.now();

            securityReportLoginService.getLoginSummary(USER_ID, periodType);

            LocalDateTime after = LocalDateTime.now();
            verify(loginActivityRepository).findBrowserCounts(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), fromCaptor.capture(), toCaptor.capture());
            assertTrue(!toCaptor.getValue().isBefore(before) && !toCaptor.getValue().isAfter(after));
        }

        @Test
        void shouldRequestSingleElementPageWhenFindingFirstAndLastLogin() {
            securityReportLoginService.getLoginSummary(USER_ID, periodType);

            verify(loginActivityRepository).findFirstLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT));
            verify(loginActivityRepository).findLastLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT));
        }
    }

    @Nested
    class GetLoginSummary {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnSummaryWhenPeriodTypeIsAnySupportedValue(PeriodType type) {
            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, type);

            assertNotNull(result);
        }

        @Test
        void shouldReturnEmptySummaryWhenNoActivitiesExist() {
            ReportLoginDto expected = new ReportLoginDto(0L, 0L, 0L, null, null, null, null, 0, List.of(), List.of());

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(expected, result);
        }

        @Test
        void shouldReturnCompleteSummaryWhenAllActivitiesExist() {
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(8L);
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(eq(USER_ID), eq(LoginActivityStatus.UNSUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(2L);
            when(loginActivityRepository.countDistinctDevices(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(3L);
            when(firstLogin.getLocation()).thenReturn("Warsaw");
            when(firstLogin.getCreatedAt()).thenReturn(FIRST_LOGIN_AT);
            when(lastLogin.getLocation()).thenReturn("Krakow");
            when(lastLogin.getCreatedAt()).thenReturn(LAST_LOGIN_AT);
            when(loginActivityRepository.findFirstLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of(firstLogin));
            when(loginActivityRepository.findLastLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of(lastLogin));
            when(loginActivityRepository.findLocationCounts(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(new LocationCountDto("Warsaw", 6L), new LocationCountDto("Krakow", 2L)));
            when(loginActivityRepository.findBrowserCounts(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(new BrowserCountDto("Chrome", 8L)));
            ReportLoginDto expected = new ReportLoginDto(8L, 2L, 3L, "Warsaw", FIRST_LOGIN_AT.toLocalDate(), "Krakow", LAST_LOGIN_AT.toLocalDate(), 2,
                    List.of(new ShareStatDto("Warsaw", CalculatePercentage.calculatePercentage(BigDecimal.valueOf(6L), BigDecimal.valueOf(8L))), new ShareStatDto("Krakow", CalculatePercentage.calculatePercentage(BigDecimal.valueOf(2L), BigDecimal.valueOf(8L)))),
                    List.of(new ShareStatDto("Chrome", CalculatePercentage.calculatePercentage(BigDecimal.valueOf(8L), BigDecimal.valueOf(8L))))
            );

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(expected, result);
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            assertThrows(NullPointerException.class, () -> securityReportLoginService.getLoginSummary(USER_ID, null));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class))).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> securityReportLoginService.getLoginSummary(USER_ID, periodType));
        }
    }
}