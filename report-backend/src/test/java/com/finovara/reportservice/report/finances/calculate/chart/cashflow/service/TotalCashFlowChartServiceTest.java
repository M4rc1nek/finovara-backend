package com.finovara.reportservice.report.finances.calculate.chart.cashflow.service;

import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.report.finances.calculate.chart.builder.CashFlowChartService;
import com.finovara.reportservice.report.finances.calculate.chart.dto.CashFlowDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TotalCashFlowChartServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private FinanceBackendReportClient reportClient;

    @Mock
    private CashFlowChartService cashFlowChartService;

    @InjectMocks
    private TotalCashFlowChartService service;

    @Nested
    class GetCashFlowChart {

        @Test
        void shouldFetchDataAndDelegateToChartBuilder() {
            List<CashFlowDto> expected = List.of();

            when(reportClient.expensesGroupedByDate(USER_ID)).thenReturn(List.of());
            when(reportClient.revenuesGroupedByDate(USER_ID)).thenReturn(List.of());
            when(cashFlowChartService.getCashFlowChart(List.of(), List.of())).thenReturn(expected);

            List<CashFlowDto> result = service.getCashFlowChart(USER_ID);

            assertThat(result).isSameAs(expected);
            verify(reportClient).expensesGroupedByDate(USER_ID);
            verify(reportClient).revenuesGroupedByDate(USER_ID);
            verify(cashFlowChartService).getCashFlowChart(List.of(), List.of());
        }
    }
}
