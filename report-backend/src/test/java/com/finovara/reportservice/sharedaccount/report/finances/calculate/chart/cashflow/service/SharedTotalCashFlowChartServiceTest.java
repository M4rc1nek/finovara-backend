package com.finovara.reportservice.sharedaccount.report.finances.calculate.chart.cashflow.service;

import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import com.finovara.reportservice.feignclient.FinanceBackendSharedReportClient;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.chart.builder.SharedCashFlowChartService;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.chart.dto.SharedCashFlowDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedTotalCashFlowChartServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;

    @Mock
    private FinanceBackendSharedReportClient reportClient;

    @Mock
    private SharedCashFlowChartService cashFlowChartService;

    @InjectMocks
    private SharedTotalCashFlowChartService sharedTotalCashFlowChartService;

    @Nested
    class GetCashFlowChart {

        @Test
        void shouldReturnChartBuiltFromExpensesAndRevenuesGroupedByDate() {
            List<DailyCashDto> expenses = List.of(mock(DailyCashDto.class));
            List<DailyCashDto> revenues = List.of(mock(DailyCashDto.class));
            List<SharedCashFlowDto> chart = List.of(mock(SharedCashFlowDto.class));
            when(reportClient.expensesGroupedByDate(OWNER_ID, MEMBER_ID)).thenReturn(expenses);
            when(reportClient.revenuesGroupedByDate(OWNER_ID, MEMBER_ID)).thenReturn(revenues);
            when(cashFlowChartService.getSharedCashFlowChart(expenses, revenues)).thenReturn(chart);

            List<SharedCashFlowDto> result = sharedTotalCashFlowChartService.getCashFlowChart(OWNER_ID, MEMBER_ID);

            assertThat(result).isEqualTo(chart);
        }

        @Test
        void shouldCallReportClientWithOwnerAndMemberIds() {
            when(reportClient.expensesGroupedByDate(OWNER_ID, MEMBER_ID)).thenReturn(Collections.emptyList());
            when(reportClient.revenuesGroupedByDate(OWNER_ID, MEMBER_ID)).thenReturn(Collections.emptyList());
            when(cashFlowChartService.getSharedCashFlowChart(Collections.emptyList(), Collections.emptyList())).thenReturn(Collections.emptyList());

            sharedTotalCashFlowChartService.getCashFlowChart(OWNER_ID, MEMBER_ID);

            verify(reportClient).expensesGroupedByDate(OWNER_ID, MEMBER_ID);
            verify(reportClient).revenuesGroupedByDate(OWNER_ID, MEMBER_ID);
        }

        @Test
        void shouldReturnEmptyChartWhenNoExpensesOrRevenuesExist() {
            when(reportClient.expensesGroupedByDate(OWNER_ID, MEMBER_ID)).thenReturn(Collections.emptyList());
            when(reportClient.revenuesGroupedByDate(OWNER_ID, MEMBER_ID)).thenReturn(Collections.emptyList());
            when(cashFlowChartService.getSharedCashFlowChart(Collections.emptyList(), Collections.emptyList())).thenReturn(Collections.emptyList());

            List<SharedCashFlowDto> result = sharedTotalCashFlowChartService.getCashFlowChart(OWNER_ID, MEMBER_ID);

            assertThat(result).isEmpty();
        }
    }
}