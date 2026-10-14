package com.finovara.reportservice.report.finances.response.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinancialReportServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private FinanceBackendReportClient financeBackendReportClient;

    @Mock
    private FinancialReportDto report;

    @InjectMocks
    private FinancialReportService service;

    @Nested
    class BuildReport {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnReportFromClientWhenPeriodTypeIsValid(PeriodType periodType) {
            when(financeBackendReportClient.getFinancialReport(USER_ID, periodType)).thenReturn(report);

            FinancialReportDto result = service.buildReport(USER_ID, periodType);

            assertEquals(report, result);
        }

        @Test
        void shouldCallClientExactlyOnceWhenPeriodTypeIsValid() {
            when(financeBackendReportClient.getFinancialReport(USER_ID, PeriodType.DAILY)).thenReturn(report);

            service.buildReport(USER_ID, PeriodType.DAILY);

            verify(financeBackendReportClient, times(1)).getFinancialReport(USER_ID, PeriodType.DAILY);
            verifyNoMoreInteractions(financeBackendReportClient);
        }

        @Test
        void shouldReturnNullWhenClientReturnsNull() {
            when(financeBackendReportClient.getFinancialReport(USER_ID, PeriodType.DAILY)).thenReturn(null);

            FinancialReportDto result = service.buildReport(USER_ID, PeriodType.DAILY);

            assertNull(result);
        }

        @Test
        void shouldPassNullUserIdToClientWhenUserIdIsNull() {
            when(financeBackendReportClient.getFinancialReport(null, PeriodType.MONTHLY)).thenReturn(report);

            FinancialReportDto result = service.buildReport(null, PeriodType.MONTHLY);

            assertEquals(report, result);
        }

        @Test
        void shouldThrowExceptionWhenClientFails() {
            when(financeBackendReportClient.getFinancialReport(USER_ID, PeriodType.WEEKLY)).thenThrow(new IllegalStateException("client error"));

            assertThrows(IllegalStateException.class, () -> service.buildReport(USER_ID, PeriodType.WEEKLY));
        }
    }

    @Nested
    class BuildReportWithInvalidInput {

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            assertThrows(InvalidInputException.class, () -> service.buildReport(USER_ID, null));
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNullAndNotCallClient() {
            assertThrows(InvalidInputException.class, () -> service.buildReport(USER_ID, null));

            verifyNoInteractions(financeBackendReportClient);
        }
    }
}