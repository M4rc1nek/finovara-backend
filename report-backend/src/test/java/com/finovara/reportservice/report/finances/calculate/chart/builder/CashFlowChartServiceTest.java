package com.finovara.reportservice.report.finances.chart.builder;

import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import com.finovara.reportservice.report.finances.calculate.chart.builder.CashFlowChartService;
import com.finovara.reportservice.report.finances.calculate.chart.dto.CashFlowDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CashFlowChartServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2024, 3, 15);
    private static final LocalDate START = LocalDate.of(2024, 3, 1);
    private static final ZoneId ZONE = ZoneId.of("UTC");

    private CashFlowChartService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE);
        service = new CashFlowChartService(fixedClock);
    }

    @Nested
    class GetCashFlowChart {

        @Nested
        class DateRangeCoverage {

            @Test
            void shouldProduceOneEntryPerDayFromStartOfMonthToToday() {
                List<CashFlowDto> result = service.getCashFlowChart(List.of(), List.of());

                assertThat(result).hasSize(15);
            }

            @Test
            void shouldStartOnFirstDayOfMonth() {
                List<CashFlowDto> result = service.getCashFlowChart(List.of(), List.of());

                assertThat(result.getFirst().date()).isEqualTo(START);
            }

            @Test
            void shouldEndOnToday() {
                List<CashFlowDto> result = service.getCashFlowChart(List.of(), List.of());

                assertThat(result.getLast().date()).isEqualTo(TODAY);
            }

            @Test
            void shouldProduceExactlyOneEntryWhenTodayIsFirstDayOfMonth() {
                LocalDate firstOfMonth = LocalDate.of(2024, 4, 1);
                Clock firstDayClock = Clock.fixed(firstOfMonth.atStartOfDay(ZONE).toInstant(), ZONE);
                CashFlowChartService firstDayService = new CashFlowChartService(firstDayClock);

                List<CashFlowDto> result = firstDayService.getCashFlowChart(List.of(), List.of());

                assertThat(result).hasSize(1);
                assertThat(result.getFirst().date()).isEqualTo(firstOfMonth);
            }

            @Test
            void shouldExcludeDatesAfterToday() {
                List<DailyCashDto> futureExpense = List.of(
                        new DailyCashDto(TODAY.plusDays(1), BigDecimal.valueOf(999)));

                List<CashFlowDto> result = service.getCashFlowChart(futureExpense, List.of());

                assertThat(result).noneMatch(dto -> dto.date().isAfter(TODAY));
            }

            @Test
            void shouldExcludeDatesBeforeStartOfMonth() {
                List<DailyCashDto> previousMonthExpense = List.of(
                        new DailyCashDto(START.minusDays(1), BigDecimal.valueOf(999)));

                List<CashFlowDto> result = service.getCashFlowChart(previousMonthExpense, List.of());

                assertThat(result).noneMatch(dto -> dto.date().isBefore(START));
            }
        }

        @Nested
        class AmountMapping {

            @Test
            void shouldMapExpenseAmountToCorrectDate() {
                List<DailyCashDto> expenses = List.of(new DailyCashDto(START, BigDecimal.valueOf(100)));

                List<CashFlowDto> result = service.getCashFlowChart(expenses, List.of());

                CashFlowDto march1 = result.getFirst();
                assertThat(march1.date()).isEqualTo(START);
                assertThat(march1.expense()).isEqualByComparingTo("100");
            }

            @Test
            void shouldMapRevenueAmountToCorrectDate() {
                List<DailyCashDto> revenues = List.of(new DailyCashDto(START, BigDecimal.valueOf(200)));

                List<CashFlowDto> result = service.getCashFlowChart(List.of(), revenues);

                CashFlowDto march1 = result.getFirst();
                assertThat(march1.date()).isEqualTo(START);
                assertThat(march1.revenue()).isEqualByComparingTo("200");
            }

            @Test
            void shouldDefaultExpenseToZeroWhenNoEntryForDate() {
                List<CashFlowDto> result = service.getCashFlowChart(List.of(), List.of());

                assertThat(result).allSatisfy(dto ->
                        assertThat(dto.expense()).isEqualByComparingTo(BigDecimal.ZERO));
            }

            @Test
            void shouldDefaultRevenueToZeroWhenNoEntryForDate() {
                List<CashFlowDto> result = service.getCashFlowChart(List.of(), List.of());

                assertThat(result).allSatisfy(dto ->
                        assertThat(dto.revenue()).isEqualByComparingTo(BigDecimal.ZERO));
            }

            @Test
            void shouldNotPolluteOtherDatesWhenOnlyOneDateHasData() {
                LocalDate activeDay = START.plusDays(2);
                List<DailyCashDto> expenses = List.of(new DailyCashDto(activeDay, BigDecimal.valueOf(50)));

                List<CashFlowDto> result = service.getCashFlowChart(expenses, List.of());

                assertThat(result)
                        .filteredOn(dto -> !dto.date().isEqual(activeDay))
                        .allSatisfy(dto -> assertThat(dto.expense()).isEqualByComparingTo(BigDecimal.ZERO));
            }
        }

        @Nested
        class Aggregation {

            @Test
            void shouldSumMultipleExpenseEntriesOnTheSameDate() {
                List<DailyCashDto> expenses = List.of(
                        new DailyCashDto(START, BigDecimal.valueOf(100)),
                        new DailyCashDto(START, BigDecimal.valueOf(50)));

                List<CashFlowDto> result = service.getCashFlowChart(expenses, List.of());

                CashFlowDto march1 = result.stream()
                        .filter(dto -> dto.date().isEqual(START))
                        .findFirst()
                        .orElseThrow();

                assertThat(march1.expense()).isEqualByComparingTo("150");
            }

            @Test
            void shouldSumMultipleRevenueEntriesOnTheSameDate() {
                List<DailyCashDto> revenues = List.of(
                        new DailyCashDto(START, BigDecimal.valueOf(200)),
                        new DailyCashDto(START, BigDecimal.valueOf(25)));

                List<CashFlowDto> result = service.getCashFlowChart(List.of(), revenues);

                CashFlowDto march1 = result.stream()
                        .filter(dto -> dto.date().isEqual(START))
                        .findFirst()
                        .orElseThrow();

                assertThat(march1.revenue()).isEqualByComparingTo("225");
            }

            @Test
            void shouldSumExpensesAndRevenuesIndependentlyOnTheSameDate() {
                List<DailyCashDto> expenses = List.of(
                        new DailyCashDto(START, BigDecimal.valueOf(40)),
                        new DailyCashDto(START, BigDecimal.valueOf(60)));
                List<DailyCashDto> revenues = List.of(
                        new DailyCashDto(START, BigDecimal.valueOf(100)),
                        new DailyCashDto(START, BigDecimal.valueOf(100)));

                List<CashFlowDto> result = service.getCashFlowChart(expenses, revenues);

                CashFlowDto march1 = result.stream()
                        .filter(dto -> dto.date().isEqual(START))
                        .findFirst()
                        .orElseThrow();

                assertThat(march1.expense()).isEqualByComparingTo("100");
                assertThat(march1.revenue()).isEqualByComparingTo("200");
            }
        }
    }
}
