package com.finovara.activitylogservice.internal.security.report;

import com.finovara.activitylogservice.internal.security.report.dto.ReportAccountChangeDto;
import com.finovara.activitylogservice.internal.security.report.dto.ReportLoginDto;
import com.finovara.activitylogservice.internal.security.report.mapper.SecurityReportMapper;
import com.finovara.activitylogservice.internal.security.report.service.SecurityReportAccountChangeService;
import com.finovara.activitylogservice.internal.security.report.service.SecurityReportLoginService;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.report.dto.SecurityReportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalSecurityReportServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private SecurityReportAccountChangeService securityReportAccountChangeService;

    @Mock
    private SecurityReportLoginService securityReportLoginService;

    @Mock
    private SecurityReportMapper securityReportMapper;

    @Mock
    private ReportAccountChangeDto reportAccountChangeDto;

    @Mock
    private ReportLoginDto reportLoginDto;

    @Mock
    private SecurityReportDto securityReportDto;

    @InjectMocks
    private InternalSecurityReportService internalSecurityReportService;

    private PeriodType periodType;

    @BeforeEach
    void setUp() {
        periodType = PeriodType.values()[0];
    }

    @Nested
    class GetSummaryForUser {

        @BeforeEach
        void stubCollaborators() {
            when(securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType)).thenReturn(reportAccountChangeDto);
            when(securityReportLoginService.getLoginSummary(USER_ID, periodType)).thenReturn(reportLoginDto);
            when(securityReportMapper.toDto(USER_ID, reportLoginDto, reportAccountChangeDto)).thenReturn(securityReportDto);
        }

        @Test
        void shouldReturnMappedReportWhenSummariesAreAvailable() {
            SecurityReportDto result = internalSecurityReportService.getSummaryForUser(USER_ID, periodType);

            assertSame(securityReportDto, result);
        }

        @Test
        void shouldFetchAccountChangesSummaryWhenReportIsRequested() {
            internalSecurityReportService.getSummaryForUser(USER_ID, periodType);

            verify(securityReportAccountChangeService).getAccountChangesSummary(USER_ID, periodType);
            verifyNoMoreInteractions(securityReportAccountChangeService);
        }

        @Test
        void shouldFetchLoginSummaryWhenReportIsRequested() {
            internalSecurityReportService.getSummaryForUser(USER_ID, periodType);

            verify(securityReportLoginService).getLoginSummary(USER_ID, periodType);
            verifyNoMoreInteractions(securityReportLoginService);
        }

        @Test
        void shouldMapBothSummariesWhenReportIsRequested() {
            internalSecurityReportService.getSummaryForUser(USER_ID, periodType);

            verify(securityReportMapper).toDto(USER_ID, reportLoginDto, reportAccountChangeDto);
            verifyNoMoreInteractions(securityReportMapper);
        }
    }

    @Nested
    class GetSummaryForUserWithAllPeriods {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnMappedReportWhenPeriodTypeIsAnySupportedValue(PeriodType type) {
            when(securityReportAccountChangeService.getAccountChangesSummary(USER_ID, type)).thenReturn(reportAccountChangeDto);
            when(securityReportLoginService.getLoginSummary(USER_ID, type)).thenReturn(reportLoginDto);
            when(securityReportMapper.toDto(USER_ID, reportLoginDto, reportAccountChangeDto)).thenReturn(securityReportDto);

            SecurityReportDto result = internalSecurityReportService.getSummaryForUser(USER_ID, type);

            assertSame(securityReportDto, result);
        }
    }

    @Nested
    class GetSummaryForUserFailures {

        @Test
        void shouldThrowExceptionWhenAccountChangeServiceFails() {
            when(securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> internalSecurityReportService.getSummaryForUser(USER_ID, periodType));
        }

        @Test
        void shouldNotCallMapperWhenAccountChangeServiceFails() {
            when(securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> internalSecurityReportService.getSummaryForUser(USER_ID, periodType));

            verifyNoMoreInteractions(securityReportMapper, securityReportLoginService);
        }

        @Test
        void shouldThrowExceptionWhenLoginServiceFails() {
            when(securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType)).thenReturn(reportAccountChangeDto);
            when(securityReportLoginService.getLoginSummary(USER_ID, periodType)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> internalSecurityReportService.getSummaryForUser(USER_ID, periodType));
        }

        @Test
        void shouldNotCallMapperWhenLoginServiceFails() {
            when(securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType)).thenReturn(reportAccountChangeDto);
            when(securityReportLoginService.getLoginSummary(USER_ID, periodType)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> internalSecurityReportService.getSummaryForUser(USER_ID, periodType));

            verifyNoMoreInteractions(securityReportMapper);
        }

        @Test
        void shouldThrowExceptionWhenMapperFails() {
            when(securityReportAccountChangeService.getAccountChangesSummary(USER_ID, periodType)).thenReturn(reportAccountChangeDto);
            when(securityReportLoginService.getLoginSummary(USER_ID, periodType)).thenReturn(reportLoginDto);
            when(securityReportMapper.toDto(USER_ID, reportLoginDto, reportAccountChangeDto)).thenThrow(new IllegalArgumentException());

            assertThrows(IllegalArgumentException.class, () -> internalSecurityReportService.getSummaryForUser(USER_ID, periodType));
        }
    }
}