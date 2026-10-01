package com.finovara.reportservice.report.security.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.report.dto.security.SecurityReportDto;
import com.finovara.reportservice.feignclient.ActivityLogBackendClient;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityReportServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private ActivityLogBackendClient activityLogBackendClient;

    @Mock
    private SecurityReportDto securityReportDto;

    @InjectMocks
    private SecurityReportService securityReportService;

    private PeriodType periodType;

    @BeforeEach
    void setUp() {
        periodType = PeriodType.values()[0];
    }

    @Nested
    class BuildReport {

        @Test
        void shouldReturnReportFromClientWhenPeriodTypeIsValid() {
            when(activityLogBackendClient.getSecurityReport(USER_ID, periodType)).thenReturn(securityReportDto);

            SecurityReportDto result = securityReportService.buildReport(USER_ID, periodType);

            assertSame(securityReportDto, result);
        }

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldDelegateToClientWhenPeriodTypeIsAnySupportedValue(PeriodType type) {
            when(activityLogBackendClient.getSecurityReport(USER_ID, type)).thenReturn(securityReportDto);

            SecurityReportDto result = securityReportService.buildReport(USER_ID, type);

            assertSame(securityReportDto, result);
            verify(activityLogBackendClient).getSecurityReport(USER_ID, type);
        }

        @Test
        void shouldCallClientExactlyOnceWhenReportIsBuilt() {
            when(activityLogBackendClient.getSecurityReport(USER_ID, periodType)).thenReturn(securityReportDto);

            securityReportService.buildReport(USER_ID, periodType);

            verify(activityLogBackendClient).getSecurityReport(USER_ID, periodType);
            verifyNoMoreInteractions(activityLogBackendClient);
        }

        @Test
        void shouldReturnNullWhenClientReturnsNull() {
            when(activityLogBackendClient.getSecurityReport(USER_ID, periodType)).thenReturn(null);

            SecurityReportDto result = securityReportService.buildReport(USER_ID, periodType);

            assertSame(null, result);
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            assertThrows(InvalidInputException.class, () -> securityReportService.buildReport(USER_ID, null));
        }

        @Test
        void shouldNotCallClientWhenPeriodTypeIsNull() {
            assertThrows(InvalidInputException.class, () -> securityReportService.buildReport(USER_ID, null));

            verifyNoInteractions(activityLogBackendClient);
        }

        @Test
        void shouldThrowExceptionWhenUserIdAndPeriodTypeAreNull() {
            assertThrows(InvalidInputException.class, () -> securityReportService.buildReport(null, null));
        }

        @Test
        void shouldPropagateExceptionWhenClientFails() {
            when(activityLogBackendClient.getSecurityReport(USER_ID, periodType)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> securityReportService.buildReport(USER_ID, periodType));
        }
    }
}