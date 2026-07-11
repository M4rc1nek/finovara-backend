package com.finovara.reportservice.report.finances.chart.averagecashflow.service;

import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.report.finances.calculate.chart.averagecashflow.service.AverageCashFlowChartService;
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
class AverageCashFlowChartServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private FinanceBackendReportClient reportClient;

    @Mock
    private CashFlowChartService cashFlowChartService;

    @InjectMocks
    private AverageCashFlowChartService service;

    @Nested
    class GetAverageCashFlowChart {

        @Test
        void shouldFetchAverageDataAndDelegateToChartBuilder() {
            List<CashFlowDto> expected = List.of();

            when(reportClient.expensesAvgGroupedByDate(USER_ID)).thenReturn(List.of());
            when(reportClient.revenuesAvgGroupedByDate(USER_ID)).thenReturn(List.of());
            when(cashFlowChartService.getCashFlowChart(List.of(), List.of())).thenReturn(expected);

            List<CashFlowDto> result = service.getAverageCashFlowChart(USER_ID);

            assertThat(result).isSameAs(expected);
            verify(reportClient).expensesAvgGroupedByDate(USER_ID);
            verify(reportClient).revenuesAvgGroupedByDate(USER_ID);
            verify(cashFlowChartService).getCashFlowChart(List.of(), List.of());
        }
    }
}
