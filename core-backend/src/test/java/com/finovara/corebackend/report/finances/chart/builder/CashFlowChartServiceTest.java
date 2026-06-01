package com.finovara.corebackend.report.finances.chart.builder;

import com.finovara.corebackend.report.finances.chart.dto.CashFlowDto;
import com.finovara.corebackend.report.finances.chart.dto.DailyCashDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
@ExtendWith(MockitoExtension.class)
class CashFlowChartServiceTest {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final LocalDate FIXED_DATE = LocalDate.of(2024, 3, 15);

    private CashFlowChartService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_DATE.atStartOfDay(ZONE).toInstant(), ZONE);
        service = new CashFlowChartService(fixedClock);
    }

    @Test
    void shouldMapExpensesAndRevenuesCorrectly() {
        LocalDate from = LocalDate.of(2024, 3, 1);

        List<DailyCashDto> expenses = List.of(
                new DailyCashDto(from, BigDecimal.valueOf(100)),
                new DailyCashDto(from.plusDays(1), BigDecimal.valueOf(50)));

        List<DailyCashDto> revenues = List.of(
                new DailyCashDto(from, BigDecimal.valueOf(200)),
                new DailyCashDto(from.plusDays(1), BigDecimal.valueOf(75)));

        List<CashFlowDto> result = service.getCashFlowChart(expenses, revenues);

        assertThat(result).hasSize(15);
        assertThat(result.getFirst().expense()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(result.getFirst().revenue()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(result.get(1).expense()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(result.get(1).revenue()).isEqualByComparingTo(BigDecimal.valueOf(75));
    }

    @Test
    void shouldReturnZerosWhenNoData() {
        List<CashFlowDto> result = service.getCashFlowChart(List.of(), List.of());

        assertThat(result).hasSize(15);
        result.forEach(dto -> {
            assertThat(dto.expense()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(dto.revenue()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }

    @Test
    void shouldSumMultipleEntriesOnSameDate() {
        LocalDate day = LocalDate.of(2024, 3, 2);

        List<DailyCashDto> expenses = List.of(
                new DailyCashDto(day, BigDecimal.valueOf(100)),
                new DailyCashDto(day, BigDecimal.valueOf(50)));

        List<DailyCashDto> revenues = List.of(
                new DailyCashDto(day, BigDecimal.valueOf(200)),
                new DailyCashDto(day, BigDecimal.valueOf(25)));

        List<CashFlowDto> result = service.getCashFlowChart(expenses, revenues);

        assertThat(result.get(1).expense()).isEqualByComparingTo(BigDecimal.valueOf(150));
        assertThat(result.get(1).revenue()).isEqualByComparingTo(BigDecimal.valueOf(225));
    }
}