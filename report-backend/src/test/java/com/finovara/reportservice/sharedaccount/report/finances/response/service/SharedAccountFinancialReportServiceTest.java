package com.finovara.reportservice.sharedaccount.report.finances.response.service;

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
import com.finovara.reportservice.feignclient.FinanceBackendSharedReportClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SharedAccountFinancialReportServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;

    @Mock
    private FinanceBackendSharedReportClient financeBackendSharedReportClient;

    @Mock
    private FinancialReportDto report;

    @InjectMocks
    private SharedAccountFinancialReportService service;

    @Nested
    class BuildReport {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnReportFromClientWhenPeriodTypeIsValid(PeriodType periodType) {
            when(financeBackendSharedReportClient.getFinancialReport(OWNER_ID, MEMBER_ID, periodType)).thenReturn(report);

            FinancialReportDto result = service.buildReport(OWNER_ID, MEMBER_ID, periodType);

            assertEquals(report, result);
        }

        @Test
        void shouldCallClientExactlyOnceWhenPeriodTypeIsValid() {
            when(financeBackendSharedReportClient.getFinancialReport(OWNER_ID, MEMBER_ID, PeriodType.DAILY)).thenReturn(report);

            service.buildReport(OWNER_ID, MEMBER_ID, PeriodType.DAILY);

            verify(financeBackendSharedReportClient, times(1)).getFinancialReport(OWNER_ID, MEMBER_ID, PeriodType.DAILY);
            verifyNoMoreInteractions(financeBackendSharedReportClient);
        }

        @Test
        void shouldReturnNullWhenClientReturnsNull() {
            when(financeBackendSharedReportClient.getFinancialReport(OWNER_ID, MEMBER_ID, PeriodType.DAILY)).thenReturn(null);

            FinancialReportDto result = service.buildReport(OWNER_ID, MEMBER_ID, PeriodType.DAILY);

            assertNull(result);
        }

        @Test
        void shouldPassNullIdsToClientWhenIdsAreNull() {
            when(financeBackendSharedReportClient.getFinancialReport(null, null, PeriodType.MONTHLY)).thenReturn(report);

            FinancialReportDto result = service.buildReport(null, null, PeriodType.MONTHLY);

            assertEquals(report, result);
        }

        @Test
        void shouldThrowExceptionWhenClientFails() {
            when(financeBackendSharedReportClient.getFinancialReport(OWNER_ID, MEMBER_ID, PeriodType.WEEKLY))
                    .thenThrow(new IllegalStateException("client error"));

            assertThrows(IllegalStateException.class, () -> service.buildReport(OWNER_ID, MEMBER_ID, PeriodType.WEEKLY));
        }
    }

    @Nested
    class BuildReportWithInvalidInput {

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            assertThrows(InvalidInputException.class, () -> service.buildReport(OWNER_ID, MEMBER_ID, null));
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNullAndNotCallClient() {
            assertThrows(InvalidInputException.class, () -> service.buildReport(OWNER_ID, MEMBER_ID, null));

            verifyNoInteractions(financeBackendSharedReportClient);
        }
    }
}