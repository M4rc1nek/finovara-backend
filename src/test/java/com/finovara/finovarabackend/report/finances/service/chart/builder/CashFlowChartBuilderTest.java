package com.finovara.finovarabackend.report.finances.service.chart.builder;

import com.finovara.finovarabackend.config.TimeConfig;
import com.finovara.finovarabackend.report.finances.chart.builder.CashFlowChartBuilder;
import com.finovara.finovarabackend.report.finances.chart.dto.CashFlowDto;
import com.finovara.finovarabackend.report.finances.chart.dto.DailyCashDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashFlowChartBuilderTest {

    @Mock
    private TimeConfig timeConfig;

    @InjectMocks
    private CashFlowChartBuilder builder;

    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(
                LocalDate.of(2024, 1, 10)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant(),
                ZoneOffset.UTC
        );

        when(timeConfig.clock()).thenReturn(clock);
    }

    @Test
    void shouldBuildCashFlowForEachDayOfMonth() {
        LocalDate today = LocalDate.now(clock);
        LocalDate startOfMonth = today.withDayOfMonth(1);

        List<DailyCashDto> expenses = List.of(
                new DailyCashDto(startOfMonth, BigDecimal.valueOf(100))
        );

        List<DailyCashDto> revenues = List.of(
                new DailyCashDto(startOfMonth, BigDecimal.valueOf(200))
        );

        List<CashFlowDto> result = builder.getCashFlowChartBuilder(expenses, revenues);

        long expectedDays = startOfMonth.datesUntil(today.plusDays(1)).count();

        assertEquals(expectedDays, result.size());
    }

    @Test
    void shouldReturnZeroWhenNoDataForDay() {
        List<DailyCashDto> expenses = List.of();
        List<DailyCashDto> revenues = List.of();

        List<CashFlowDto> result = builder.getCashFlowChartBuilder(expenses, revenues);

        result.forEach(day -> {
            assertEquals(BigDecimal.ZERO, day.expense());
            assertEquals(BigDecimal.ZERO, day.revenue());
        });
    }

    @Test
    void shouldSumValuesForSameDay() {
        LocalDate today = LocalDate.now(clock);

        List<DailyCashDto> expenses = List.of(
                new DailyCashDto(today, BigDecimal.valueOf(100)),
                new DailyCashDto(today, BigDecimal.valueOf(50))
        );

        List<DailyCashDto> revenues = List.of(
                new DailyCashDto(today, BigDecimal.valueOf(200)),
                new DailyCashDto(today, BigDecimal.valueOf(300))
        );

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
        LocalDate today = LocalDate.now(clock);

        List<DailyCashDto> expenses = List.of(
                new DailyCashDto(today.minusDays(1), BigDecimal.valueOf(100))
        );

        List<DailyCashDto> revenues = List.of(
                new DailyCashDto(today, BigDecimal.valueOf(200))
        );

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