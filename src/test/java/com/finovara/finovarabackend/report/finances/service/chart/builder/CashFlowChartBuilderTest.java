package com.finovara.finovarabackend.report.finances.service.chart.builder;

import com.finovara.finovarabackend.report.finances.chart.builder.CashFlowChartBuilder;
import com.finovara.finovarabackend.report.finances.chart.dto.CashFlowDto;
import com.finovara.finovarabackend.report.finances.chart.dto.DailyCashDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CashFlowChartBuilderTest {

    private CashFlowChartBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new CashFlowChartBuilder();
    }

    @Test
    void shouldBuildCashFlowForEachDayOfMonth() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        List<DailyCashDto> expenses = List.of(new DailyCashDto(startOfMonth, BigDecimal.valueOf(100)));

        List<DailyCashDto> revenues = List.of(new DailyCashDto(startOfMonth, BigDecimal.valueOf(200)));

        List<CashFlowDto> result = builder.getCashFlowChartBuilder(expenses, revenues);

        long expectedDays = startOfMonth.datesUntil(today.plusDays(1)).count();

        assertEquals(expectedDays, result.size());
    }

    @Test
    void shouldReturnZeroWhenNoDataForDay() {
        List<DailyCashDto> expenses = List.of();
        List<DailyCashDto> revenues = List.of();

        List<CashFlowDto> result = builder.getCashFlowChartBuilder(expenses, revenues);

        result.forEach(day -> {assertEquals(BigDecimal.ZERO, day.expense());
            assertEquals(BigDecimal.ZERO, day.revenue());
        });
    }

    @Test
    void shouldSumValuesForSameDay() {
        LocalDate today = LocalDate.now();

        List<DailyCashDto> expenses = List.of(new DailyCashDto(today, BigDecimal.valueOf(100)),
                new DailyCashDto(today, BigDecimal.valueOf(50)));

        List<DailyCashDto> revenues = List.of(new DailyCashDto(today, BigDecimal.valueOf(200)),
                new DailyCashDto(today, BigDecimal.valueOf(300)));

        List<CashFlowDto> result = builder.getCashFlowChartBuilder(expenses, revenues);

        CashFlowDto todayData = result.stream()
                .filter(d -> d.date().equals(today))
                .findFirst()
                .orElseThrow();

        assertEquals(BigDecimal.valueOf(150), todayData.expense());
        assertEquals(BigDecimal.valueOf(500), todayData.revenue());
    }

    @Test
    void shouldMatchCorrectDates() {
        LocalDate today = LocalDate.now();

        List<DailyCashDto> expenses = List.of(new DailyCashDto(today.minusDays(1), BigDecimal.valueOf(100)));

        List<DailyCashDto> revenues = List.of(new DailyCashDto(today, BigDecimal.valueOf(200)));

        List<CashFlowDto> result = builder.getCashFlowChartBuilder(expenses, revenues);

        CashFlowDto yesterday = result.stream()
                .filter(d -> d.date().equals(today.minusDays(1)))
                .findFirst()
                .orElseThrow();

        CashFlowDto todayData = result.stream()
                .filter(d -> d.date().equals(today))
                .findFirst()
                .orElseThrow();

        assertEquals(BigDecimal.valueOf(100), yesterday.expense());
        assertEquals(BigDecimal.ZERO, yesterday.revenue());

        assertEquals(BigDecimal.ZERO, todayData.expense());
        assertEquals(BigDecimal.valueOf(200), todayData.revenue());
    }
}

