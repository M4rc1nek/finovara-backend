package com.finovara.finovarabackend.report.finances.service.chart.builder;


import com.finovara.finovarabackend.report.finances.chart.builder.CashFlowChartService;
import com.finovara.finovarabackend.report.finances.chart.dto.CashFlowDto;
import com.finovara.finovarabackend.report.finances.chart.dto.DailyCashDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CashFlowChartServiceTest {

    @InjectMocks
    private CashFlowChartService cashFlowChartService;

    @BeforeEach
    void setUp() {
        cashFlowChartService = new CashFlowChartService();
    }

    @Test
    void shouldMapExpensesAndRevenuesCorrectly() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        List<DailyCashDto> expenses = List.of(
                new DailyCashDto(startOfMonth, BigDecimal.valueOf(100)),
                new DailyCashDto(startOfMonth.plusDays(1), BigDecimal.valueOf(50)));

        List<DailyCashDto> revenues = List.of(
                new DailyCashDto(startOfMonth, BigDecimal.valueOf(200)),
                new DailyCashDto(startOfMonth.plusDays(1), BigDecimal.valueOf(75)));

        List<CashFlowDto> result = cashFlowChartService.getCashFlowChart(expenses, revenues);

        assertThat(result).hasSize(today.getDayOfMonth());

        assertThat(result.getFirst().date()).isEqualTo(startOfMonth);
        assertThat(result.getFirst().expense()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(result.getFirst().revenue()).isEqualByComparingTo(BigDecimal.valueOf(200));

        assertThat(result.get(1).date()).isEqualTo(startOfMonth.plusDays(1));
        assertThat(result.get(1).expense()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(result.get(1).revenue()).isEqualByComparingTo(BigDecimal.valueOf(75));

        for (int i = 2; i < result.size(); i++) {
            assertThat(result.get(i).expense()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.get(i).revenue()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Test
    void shouldReturnEmptyListWhenNoExpensesOrRevenues() {
        List<CashFlowDto> result = cashFlowChartService.getCashFlowChart(List.of(), List.of());

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        assertThat(result).hasSize(today.getDayOfMonth());

        result.forEach(dto -> {
            assertThat(dto.revenue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(dto.expense()).isEqualByComparingTo(BigDecimal.ZERO);
        });

        for (int i = 0; i < result.size(); i++) {
            assertThat(result.get(i).date()).isEqualTo(startOfMonth.plusDays(i));
        }
    }

    @Test
    void shouldSumMultipleEntriesOnSameDate() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        List<DailyCashDto> expenses = List.of(
                new DailyCashDto(startOfMonth, BigDecimal.valueOf(100)),
                new DailyCashDto(startOfMonth, BigDecimal.valueOf(50))
        );

        List<DailyCashDto> revenues = List.of(
                new DailyCashDto(startOfMonth, BigDecimal.valueOf(200)),
                new DailyCashDto(startOfMonth, BigDecimal.valueOf(25))
        );

        List<CashFlowDto> result = cashFlowChartService.getCashFlowChart(expenses, revenues);

        assertThat(result.getFirst().expense()).isEqualByComparingTo(BigDecimal.valueOf(150));
        assertThat(result.getFirst().revenue()).isEqualByComparingTo(BigDecimal.valueOf(225));
    }
}