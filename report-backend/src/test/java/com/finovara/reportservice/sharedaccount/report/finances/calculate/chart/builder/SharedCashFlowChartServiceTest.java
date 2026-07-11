package com.finovara.reportservice.sharedaccount.report.finances.calculate.chart.builder;

import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.chart.dto.SharedCashFlowDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SharedCashFlowChartServiceTest {

    private Clock clock;
    private SharedCashFlowChartService sharedCashFlowChartService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-07-11T00:00:00Z"), ZoneOffset.UTC);
        sharedCashFlowChartService = new SharedCashFlowChartService(clock);
    }

    @Nested
    class GetSharedCashFlowChartWithoutToday {

        @Test
        void shouldUseClockDateAsTodayWhenNotProvided() {
            LocalDate today = LocalDate.now(clock);

            List<SharedCashFlowDto> result = sharedCashFlowChartService.getSharedCashFlowChart(Collections.emptyList(), Collections.emptyList());

            assertThat(result).isNotEmpty();
            assertThat(result.get(result.size() - 1).date()).isEqualTo(today);
        }

        @Test
        void shouldReturnEntryForEveryDayFromStartOfMonthToTodayFromClock() {
            LocalDate today = LocalDate.now(clock);

            List<SharedCashFlowDto> result = sharedCashFlowChartService.getSharedCashFlowChart(Collections.emptyList(), Collections.emptyList());

            assertThat(result).hasSize(today.getDayOfMonth());
        }
    }

    @Nested
    class GetSharedCashFlowChartWithToday {

        @Test
        void shouldReturnEntryForEveryDayFromStartOfMonthToToday() {
            LocalDate today = LocalDate.of(2026, 7, 11);

            List<SharedCashFlowDto> result = sharedCashFlowChartService.getSharedCashFlowChart(Collections.emptyList(), Collections.emptyList(), today);

            assertThat(result).hasSize(11);
            assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(result.get(result.size() - 1).date()).isEqualTo(today);
        }

        @Test
        void shouldReturnSingleEntryWhenTodayIsFirstDayOfMonth() {
            LocalDate today = LocalDate.of(2026, 7, 1);

            List<SharedCashFlowDto> result = sharedCashFlowChartService.getSharedCashFlowChart(Collections.emptyList(), Collections.emptyList(), today);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).date()).isEqualTo(today);
        }

        @Test
        void shouldDefaultToZeroWhenNoExpenseOrRevenueForDate() {
            LocalDate today = LocalDate.of(2026, 7, 3);

            List<SharedCashFlowDto> result = sharedCashFlowChartService.getSharedCashFlowChart(Collections.emptyList(), Collections.emptyList(), today);

            assertThat(result).allSatisfy(entry -> {
                assertThat(entry.expense()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(entry.revenue()).isEqualByComparingTo(BigDecimal.ZERO);
            });
        }

        @Test
        void shouldMapExpenseAmountToCorrectDate() {
            LocalDate today = LocalDate.of(2026, 7, 5);
            LocalDate expenseDate = LocalDate.of(2026, 7, 3);
            List<DailyCashDto> expenses = List.of(new DailyCashDto(expenseDate, new BigDecimal("50")));

            List<SharedCashFlowDto> result = sharedCashFlowChartService.getSharedCashFlowChart(expenses, Collections.emptyList(), today);

            SharedCashFlowDto entry = findByDate(result, expenseDate);
            assertThat(entry.expense()).isEqualByComparingTo("50");
            assertThat(entry.revenue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldMapRevenueAmountToCorrectDate() {
            LocalDate today = LocalDate.of(2026, 7, 5);
            LocalDate revenueDate = LocalDate.of(2026, 7, 2);
            List<DailyCashDto> revenues = List.of(new DailyCashDto(revenueDate, new BigDecimal("80")));

            List<SharedCashFlowDto> result = sharedCashFlowChartService.getSharedCashFlowChart(Collections.emptyList(), revenues, today);

            SharedCashFlowDto entry = findByDate(result, revenueDate);
            assertThat(entry.revenue()).isEqualByComparingTo("80");
            assertThat(entry.expense()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldSumDuplicateExpenseEntriesForSameDate() {
            LocalDate today = LocalDate.of(2026, 7, 5);
            LocalDate date = LocalDate.of(2026, 7, 4);
            List<DailyCashDto> expenses = List.of(
                    new DailyCashDto(date, new BigDecimal("20")),
                    new DailyCashDto(date, new BigDecimal("30"))
            );

            List<SharedCashFlowDto> result = sharedCashFlowChartService.getSharedCashFlowChart(expenses, Collections.emptyList(), today);

            SharedCashFlowDto entry = findByDate(result, date);
            assertThat(entry.expense()).isEqualByComparingTo("50");
        }

        @Test
        void shouldSumDuplicateRevenueEntriesForSameDate() {
            LocalDate today = LocalDate.of(2026, 7, 5);
            LocalDate date = LocalDate.of(2026, 7, 4);
            List<DailyCashDto> revenues = List.of(
                    new DailyCashDto(date, new BigDecimal("15")),
                    new DailyCashDto(date, new BigDecimal("25"))
            );

            List<SharedCashFlowDto> result = sharedCashFlowChartService.getSharedCashFlowChart(Collections.emptyList(), revenues, today);

            SharedCashFlowDto entry = findByDate(result, date);
            assertThat(entry.revenue()).isEqualByComparingTo("40");
        }

        @Test
        void shouldNotIncludeExpenseDatesOutsideCurrentMonthRange() {
            LocalDate today = LocalDate.of(2026, 7, 5);
            LocalDate outsideDate = LocalDate.of(2026, 6, 30);
            List<DailyCashDto> expenses = List.of(new DailyCashDto(outsideDate, BigDecimal.TEN));

            List<SharedCashFlowDto> result = sharedCashFlowChartService.getSharedCashFlowChart(expenses, Collections.emptyList(), today);

            assertThat(result).noneMatch(entry -> entry.date().equals(outsideDate));
        }

        @Test
        void shouldNotIncludeExpenseDatesAfterToday() {
            LocalDate today = LocalDate.of(2026, 7, 5);
            LocalDate futureDate = LocalDate.of(2026, 7, 6);
            List<DailyCashDto> expenses = List.of(new DailyCashDto(futureDate, BigDecimal.TEN));

            List<SharedCashFlowDto> result = sharedCashFlowChartService.getSharedCashFlowChart(expenses, Collections.emptyList(), today);

            assertThat(result).noneMatch(entry -> entry.date().equals(futureDate));
        }

        @Test
        void shouldHandleLeapYearFebruary() {
            LocalDate today = LocalDate.of(2028, 2, 29);

            List<SharedCashFlowDto> result = sharedCashFlowChartService.getSharedCashFlowChart(Collections.emptyList(), Collections.emptyList(), today);

            assertThat(result).hasSize(29);
            assertThat(result.get(result.size() - 1).date()).isEqualTo(today);
        }

        @Test
        void shouldCombineExpenseAndRevenueOnSameDateIndependently() {
            LocalDate today = LocalDate.of(2026, 7, 5);
            LocalDate date = LocalDate.of(2026, 7, 3);
            List<DailyCashDto> expenses = List.of(new DailyCashDto(date, new BigDecimal("10")));
            List<DailyCashDto> revenues = List.of(new DailyCashDto(date, new BigDecimal("40")));

            List<SharedCashFlowDto> result = sharedCashFlowChartService.getSharedCashFlowChart(expenses, revenues, today);

            SharedCashFlowDto entry = findByDate(result, date);
            assertThat(entry.expense()).isEqualByComparingTo("10");
            assertThat(entry.revenue()).isEqualByComparingTo("40");
        }

        private SharedCashFlowDto findByDate(List<SharedCashFlowDto> result, LocalDate date) {
            return result.stream()
                    .filter(entry -> entry.date().equals(date))
                    .findFirst()
                    .orElseThrow();
        }
    }
}