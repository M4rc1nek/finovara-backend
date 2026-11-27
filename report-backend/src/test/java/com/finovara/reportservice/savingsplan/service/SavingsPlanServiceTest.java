package com.finovara.reportservice.savingsplan.service;

import com.finovara.contracts.mainaccount.report.savingsplan.dto.SavingsPlanMonthDto;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.savingsplan.dto.request.SavingsPlanRequest;
import com.finovara.reportservice.savingsplan.dto.response.SavingsPlanReport;
import com.finovara.reportservice.savingsplan.service.calculation.SavingsPlanCalculator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavingsPlanServiceTest {

    @Mock
    private FinanceBackendReportClient financeBackendReportClient;

    @Mock
    private SavingsPlanCalculator savingsPlanCalculator;

    @InjectMocks
    private SavingsPlanService savingsPlanService;

    @Nested
    class CreateReport {

        @Test
        void shouldLoadMostRecentRevenueMonthForCurrentUserWhenCreatingReport() {
            SavingsPlanRequest request = new SavingsPlanRequest(null, null, null, null, null);
            List<SavingsPlanMonthDto> data = List.of();
            when(financeBackendReportClient.getSavingsPlanMonthlyData(42L)).thenReturn(data);
            when(savingsPlanCalculator.calculate(eq(request), eq(data), any(LocalDate.class)))
                    .thenReturn(new SavingsPlanReport(
                            null, null, null, null, null, null, null, null, null,
                            null, null, List.of(), List.of(), false, null));

            savingsPlanService.createReport(42L, request);

            verify(financeBackendReportClient).getSavingsPlanMonthlyData(42L);
            verify(savingsPlanCalculator).calculate(eq(request), eq(data), any(LocalDate.class));
        }
    }
}
