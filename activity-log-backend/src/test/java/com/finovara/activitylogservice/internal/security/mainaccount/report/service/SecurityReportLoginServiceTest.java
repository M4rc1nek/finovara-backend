package com.finovara.activitylogservice.internal.security.mainaccount.report.service;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.model.LoginActivity;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.repository.LoginActivityRepository;
import com.finovara.activitylogservice.internal.security.mainaccount.report.dto.ReportLoginDto;
import com.finovara.activitylogservice.internal.security.util.clientinfo.ClientInfoResolver;
import com.finovara.activitylogservice.internal.security.util.clientinfo.dto.ClientInfoDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.mainaccount.activity.model.LoginActivityStatus;
import com.finovara.contracts.mainaccount.report.security.dto.ShareStatDto;
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

import static com.finovara.contracts.util.calculate.CalculatePercentage.calculatePercentage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
    private ClientInfoResolver clientInfoResolver;

    @Mock
    private ClientInfoDto clientInfoDto;

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

        @BeforeEach
        void setUp() {
            when(clientInfoResolver.getClientContext(eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(clientInfoDto);
            when(clientInfoDto.locationShares()).thenReturn(List.of());
            when(clientInfoDto.browserShares()).thenReturn(List.of());
        }

        @Test
        void shouldReturnSuccessfulLoginsCountWhenLoginsExist() {
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(10L);
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(eq(USER_ID), eq(LoginActivityStatus.UNSUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(10L, result.successfulLogins());
        }

        @Test
        void shouldReturnFailedLoginsCountWhenFailedLoginsExist() {
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(eq(USER_ID), eq(LoginActivityStatus.UNSUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(4L);

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(4L, result.failedLogins());
        }

        @Test
        void shouldKeepSuccessfulAndFailedCountsSeparateWhenBothExist() {
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(7L);
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(eq(USER_ID), eq(LoginActivityStatus.UNSUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(2L);

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(7L, result.successfulLogins());
            assertEquals(2L, result.failedLogins());
        }

        @Test
        void shouldReturnZeroCountsWhenNoLoginsExist() {
            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(0L, result.successfulLogins());
            assertEquals(0L, result.failedLogins());
        }

        @Test
        void shouldReturnMaxLongCountWhenCountIsAtUpperBound() {
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(Long.MAX_VALUE);
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(eq(USER_ID), eq(LoginActivityStatus.UNSUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(Long.MAX_VALUE, result.successfulLogins());
        }

        @Test
        void shouldReturnKnownDevicesCountWhenDevicesExist() {
            when(loginActivityRepository.countDistinctDevices(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(3L);

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(3L, result.knownDevicesCount());
        }

        @Test
        void shouldReturnZeroKnownDevicesCountWhenNoDevicesExist() {
            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(0L, result.knownDevicesCount());
        }
    }

    @Nested
    class FirstAndLastLogin {

        @BeforeEach
        void setUp() {
            when(clientInfoResolver.getClientContext(eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(clientInfoDto);
            when(clientInfoDto.locationShares()).thenReturn(List.of());
            when(clientInfoDto.browserShares()).thenReturn(List.of());
        }

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
        void shouldReturnNullFirstLoginLocationWhenFirstLoginHasNoLocation() {
            when(firstLogin.getLocation()).thenReturn(null);
            when(firstLogin.getCreatedAt()).thenReturn(FIRST_LOGIN_AT);
            when(loginActivityRepository.findFirstLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of(firstLogin));

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertNull(result.firstLoginFrom());
            assertEquals(FIRST_LOGIN_AT.toLocalDate(), result.firstLoginAt());
        }

        @Test
        void shouldReturnNullLastLoginLocationWhenLastLoginHasNoLocation() {
            when(lastLogin.getLocation()).thenReturn(null);
            when(lastLogin.getCreatedAt()).thenReturn(LAST_LOGIN_AT);
            when(loginActivityRepository.findLastLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of(lastLogin));

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertNull(result.lastLoginFrom());
            assertEquals(LAST_LOGIN_AT.toLocalDate(), result.lastLoginAt());
        }

        @Test
        void shouldKeepFirstAndLastLoginIndependentWhenOnlyLastLoginExists() {
            when(lastLogin.getLocation()).thenReturn("Krakow");
            when(lastLogin.getCreatedAt()).thenReturn(LAST_LOGIN_AT);
            when(loginActivityRepository.findLastLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of(lastLogin));

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertNull(result.firstLoginFrom());
            assertNull(result.firstLoginAt());
            assertEquals("Krakow", result.lastLoginFrom());
        }

        @Test
        void shouldKeepFirstAndLastLoginIndependentWhenOnlyFirstLoginExists() {
            when(firstLogin.getLocation()).thenReturn("Warsaw");
            when(firstLogin.getCreatedAt()).thenReturn(FIRST_LOGIN_AT);
            when(loginActivityRepository.findFirstLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of(firstLogin));

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals("Warsaw", result.firstLoginFrom());
            assertNull(result.lastLoginFrom());
            assertNull(result.lastLoginAt());
        }

        @Test
        void shouldUseFirstElementWhenRepositoryReturnsMultipleFirstLogins() {
            LoginActivity secondLogin = org.mockito.Mockito.mock(LoginActivity.class);
            when(firstLogin.getLocation()).thenReturn("Warsaw");
            when(firstLogin.getCreatedAt()).thenReturn(FIRST_LOGIN_AT);
            when(loginActivityRepository.findFirstLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of(firstLogin, secondLogin));

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals("Warsaw", result.firstLoginFrom());
        }
    }

    @Nested
    class ClientInfoShares {

        private List<ShareStatDto> locationShares;
        private List<ShareStatDto> browserShares;

        @BeforeEach
        void setUp() {
            locationShares = List.of(
                    new ShareStatDto("Warsaw", calculatePercentage(BigDecimal.valueOf(3L), BigDecimal.valueOf(4L))),
                    new ShareStatDto("Krakow", calculatePercentage(BigDecimal.valueOf(1L), BigDecimal.valueOf(4L)))
            );
            browserShares = List.of(
                    new ShareStatDto("Chrome", calculatePercentage(BigDecimal.valueOf(6L), BigDecimal.valueOf(8L))),
                    new ShareStatDto("Firefox", calculatePercentage(BigDecimal.valueOf(2L), BigDecimal.valueOf(8L)))
            );
            when(clientInfoResolver.getClientContext(eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(clientInfoDto);
        }

        @Test
        void shouldReturnLocationSharesFromClientInfoWhenLocationsExist() {
            when(clientInfoDto.locationShares()).thenReturn(locationShares);
            when(clientInfoDto.browserShares()).thenReturn(List.of());

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(locationShares, result.locationShares());
        }

        @Test
        void shouldReturnBrowserSharesFromClientInfoWhenBrowsersExist() {
            when(clientInfoDto.locationShares()).thenReturn(List.of());
            when(clientInfoDto.browserShares()).thenReturn(browserShares);

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(browserShares, result.browserShares());
        }

        @Test
        void shouldReturnDistinctLocationsCountWhenLocationsExist() {
            when(clientInfoDto.locationShares()).thenReturn(locationShares);
            when(clientInfoDto.browserShares()).thenReturn(List.of());

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(2, result.distinctLocationsCount());
        }

        @Test
        void shouldReturnEmptySharesAndZeroDistinctLocationsWhenNoClientDataExists() {
            when(clientInfoDto.locationShares()).thenReturn(List.of());
            when(clientInfoDto.browserShares()).thenReturn(List.of());

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(0, result.distinctLocationsCount());
            assertTrue(result.locationShares().isEmpty());
            assertTrue(result.browserShares().isEmpty());
        }

        @Test
        void shouldNotAffectLocationSharesWhenOnlyBrowsersExist() {
            when(clientInfoDto.locationShares()).thenReturn(List.of());
            when(clientInfoDto.browserShares()).thenReturn(browserShares);

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertTrue(result.locationShares().isEmpty());
            assertEquals(0, result.distinctLocationsCount());
        }

        @Test
        void shouldNotAffectBrowserSharesWhenOnlyLocationsExist() {
            when(clientInfoDto.locationShares()).thenReturn(locationShares);
            when(clientInfoDto.browserShares()).thenReturn(List.of());

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertTrue(result.browserShares().isEmpty());
        }

        @Test
        void shouldReturnSameShareInstancesWhenClientInfoProvidesShares() {
            when(clientInfoDto.locationShares()).thenReturn(locationShares);
            when(clientInfoDto.browserShares()).thenReturn(browserShares);

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertSame(locationShares, result.locationShares());
            assertSame(browserShares, result.browserShares());
        }
    }

    @Nested
    class DateRange {

        @BeforeEach
        void setUp() {
            when(clientInfoResolver.getClientContext(eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(clientInfoDto);
            when(clientInfoDto.locationShares()).thenReturn(List.of());
            when(clientInfoDto.browserShares()).thenReturn(List.of());
        }

        @Test
        void shouldQueryFromStartOfPeriodWhenSummaryIsRequested() {
            LocalDateTime expectedFrom = periodType.getStartDate(LocalDate.now()).atStartOfDay();

            securityReportLoginService.getLoginSummary(USER_ID, periodType);

            verify(loginActivityRepository).countDistinctDevices(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), fromCaptor.capture(), toCaptor.capture());
            assertEquals(expectedFrom, fromCaptor.getValue());
        }

        @Test
        void shouldQueryUntilCurrentMomentWhenSummaryIsRequested() {
            LocalDateTime before = LocalDateTime.now();

            securityReportLoginService.getLoginSummary(USER_ID, periodType);

            LocalDateTime after = LocalDateTime.now();
            verify(loginActivityRepository).countDistinctDevices(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), fromCaptor.capture(), toCaptor.capture());
            assertTrue(!toCaptor.getValue().isBefore(before) && !toCaptor.getValue().isAfter(after));
        }

        @Test
        void shouldPassSameDateRangeToClientInfoResolverWhenSummaryIsRequested() {
            securityReportLoginService.getLoginSummary(USER_ID, periodType);

            verify(loginActivityRepository).countDistinctDevices(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), fromCaptor.capture(), toCaptor.capture());
            verify(clientInfoResolver).getClientContext(USER_ID, fromCaptor.getValue(), toCaptor.getValue());
        }

        @Test
        void shouldPassSameDateRangeToFirstAndLastLoginQueriesWhenSummaryIsRequested() {
            securityReportLoginService.getLoginSummary(USER_ID, periodType);

            verify(loginActivityRepository).countDistinctDevices(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), fromCaptor.capture(), toCaptor.capture());
            verify(loginActivityRepository).findFirstLogins(USER_ID, LoginActivityStatus.SUCCESSFUL, fromCaptor.getValue(), toCaptor.getValue(), FIRST_ELEMENT);
            verify(loginActivityRepository).findLastLogins(USER_ID, LoginActivityStatus.SUCCESSFUL, fromCaptor.getValue(), toCaptor.getValue(), FIRST_ELEMENT);
        }

        @Test
        void shouldRequestSingleElementPageWhenFindingFirstAndLastLogin() {
            securityReportLoginService.getLoginSummary(USER_ID, periodType);

            verify(loginActivityRepository).findFirstLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT));
            verify(loginActivityRepository).findLastLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT));
        }

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldQueryFromStartOfPeriodWhenPeriodTypeIsAnySupportedValue(PeriodType type) {
            LocalDateTime expectedFrom = type.getStartDate(LocalDate.now()).atStartOfDay();

            securityReportLoginService.getLoginSummary(USER_ID, type);

            verify(loginActivityRepository).countDistinctDevices(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), fromCaptor.capture(), toCaptor.capture());
            assertEquals(expectedFrom, fromCaptor.getValue());
        }
    }

    @Nested
    class RepositoryInteractions {

        @BeforeEach
        void setUp() {
            when(clientInfoResolver.getClientContext(eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(clientInfoDto);
            when(clientInfoDto.locationShares()).thenReturn(List.of());
            when(clientInfoDto.browserShares()).thenReturn(List.of());
        }

        @Test
        void shouldQueryEachStatusCountOnceWhenSummaryIsRequested() {
            securityReportLoginService.getLoginSummary(USER_ID, periodType);

            verify(loginActivityRepository).countByUserIdAndStatusAndCreatedAtBetween(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class));
            verify(loginActivityRepository).countByUserIdAndStatusAndCreatedAtBetween(eq(USER_ID), eq(LoginActivityStatus.UNSUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class));
        }

        @Test
        void shouldCallOnlyExpectedRepositoryMethodsWhenSummaryIsRequested() {
            securityReportLoginService.getLoginSummary(USER_ID, periodType);

            verify(loginActivityRepository).countByUserIdAndStatusAndCreatedAtBetween(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class));
            verify(loginActivityRepository).countByUserIdAndStatusAndCreatedAtBetween(eq(USER_ID), eq(LoginActivityStatus.UNSUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class));
            verify(loginActivityRepository).countDistinctDevices(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class));
            verify(loginActivityRepository).findFirstLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT));
            verify(loginActivityRepository).findLastLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT));
            verifyNoMoreInteractions(loginActivityRepository);
        }

        @Test
        void shouldResolveClientContextOnceForUserWhenSummaryIsRequested() {
            securityReportLoginService.getLoginSummary(USER_ID, periodType);

            verify(clientInfoResolver).getClientContext(eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class));
            verifyNoMoreInteractions(clientInfoResolver);
        }
    }

    @Nested
    class GetLoginSummary {

        @BeforeEach
        void setUp() {
            when(clientInfoResolver.getClientContext(eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(clientInfoDto);
            when(clientInfoDto.locationShares()).thenReturn(List.of());
            when(clientInfoDto.browserShares()).thenReturn(List.of());
        }

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
            List<ShareStatDto> locationShares = List.of(
                    new ShareStatDto("Warsaw", calculatePercentage(BigDecimal.valueOf(6L), BigDecimal.valueOf(8L))),
                    new ShareStatDto("Krakow", calculatePercentage(BigDecimal.valueOf(2L), BigDecimal.valueOf(8L)))
            );
            List<ShareStatDto> browserShares = List.of(
                    new ShareStatDto("Chrome", calculatePercentage(BigDecimal.valueOf(8L), BigDecimal.valueOf(8L)))
            );
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(8L);
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(eq(USER_ID), eq(LoginActivityStatus.UNSUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(2L);
            when(loginActivityRepository.countDistinctDevices(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(3L);
            when(firstLogin.getLocation()).thenReturn("Warsaw");
            when(firstLogin.getCreatedAt()).thenReturn(FIRST_LOGIN_AT);
            when(lastLogin.getLocation()).thenReturn("Krakow");
            when(lastLogin.getCreatedAt()).thenReturn(LAST_LOGIN_AT);
            when(loginActivityRepository.findFirstLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of(firstLogin));
            when(loginActivityRepository.findLastLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of(lastLogin));
            when(clientInfoDto.locationShares()).thenReturn(locationShares);
            when(clientInfoDto.browserShares()).thenReturn(browserShares);
            ReportLoginDto expected = new ReportLoginDto(8L, 2L, 3L, "Warsaw", FIRST_LOGIN_AT.toLocalDate(), "Krakow", LAST_LOGIN_AT.toLocalDate(), 2, locationShares, browserShares);

            ReportLoginDto result = securityReportLoginService.getLoginSummary(USER_ID, periodType);

            assertEquals(expected, result);
        }
    }

    @Nested
    class Exceptions {

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            assertThrows(NullPointerException.class, () -> securityReportLoginService.getLoginSummary(USER_ID, null));
        }

        @Test
        void shouldThrowExceptionWhenSuccessfulLoginsCountFails() {
            when(loginActivityRepository.countByUserIdAndStatusAndCreatedAtBetween(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class))).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> securityReportLoginService.getLoginSummary(USER_ID, periodType));
        }

        @Test
        void shouldThrowExceptionWhenKnownDevicesCountFails() {
            when(loginActivityRepository.countDistinctDevices(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class))).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> securityReportLoginService.getLoginSummary(USER_ID, periodType));
        }

        @Test
        void shouldThrowExceptionWhenFindingFirstLoginFails() {
            when(loginActivityRepository.findFirstLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> securityReportLoginService.getLoginSummary(USER_ID, periodType));
        }

        @Test
        void shouldThrowExceptionWhenFindingLastLoginFails() {
            when(loginActivityRepository.findLastLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> securityReportLoginService.getLoginSummary(USER_ID, periodType));
        }

        @Test
        void shouldThrowExceptionWhenClientInfoResolverFails() {
            when(clientInfoResolver.getClientContext(eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class))).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> securityReportLoginService.getLoginSummary(USER_ID, periodType));
        }

        @Test
        void shouldThrowExceptionWhenClientInfoIsNull() {
            assertThrows(NullPointerException.class, () -> securityReportLoginService.getLoginSummary(USER_ID, periodType));
        }

        @Test
        void shouldThrowExceptionWhenFirstLoginHasNoCreationDate() {
            when(loginActivityRepository.findFirstLogins(eq(USER_ID), eq(LoginActivityStatus.SUCCESSFUL), any(LocalDateTime.class), any(LocalDateTime.class), eq(FIRST_ELEMENT))).thenReturn(List.of(firstLogin));

            assertThrows(NullPointerException.class, () -> securityReportLoginService.getLoginSummary(USER_ID, periodType));
        }
    }
}