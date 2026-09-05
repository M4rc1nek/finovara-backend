package com.finovara.reportservice.healthscore.service;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.healthscore.dto.HealthScoreDto;
import com.finovara.reportservice.healthscore.model.HealthScoreStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthScoreServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private FinanceBackendReportClient reportClient;

    @InjectMocks
    private HealthScoreService healthScoreService;

    private LocalDate from;
    private LocalDate to;

    @BeforeEach
    void setUp() {
        to = LocalDate.now();
        from = to.withDayOfMonth(1);
    }

    @Nested
    class GetHealthScore {

        @Test
        void shouldReturnExcellentStatusWhenAllComponentScoresAreAtMaximum() {
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(1000));
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.ZERO, BigDecimal.valueOf(100));
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.valueOf(1000));
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.highestExpenses(USER_ID, from, to, 5)).thenReturn(Collections.emptyList());

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertNotNull(result);
            assertEquals(0, result.finalScore().compareTo(BigDecimal.valueOf(100)));
            assertEquals(HealthScoreStatus.EXCELLENT, result.statusFromScore());
        }

        @Test
        void shouldTreatNullRevenuesAsZeroSavingsScore() {
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(null);
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.ZERO);
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.ZERO);
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.ZERO);

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertEquals(0, result.finalScore().compareTo(BigDecimal.valueOf(60)));
            assertEquals(HealthScoreStatus.NEEDS_ATTENTION, result.statusFromScore());
        }

        @Test
        void shouldTreatZeroRevenuesAsZeroSavingsScore() {
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(BigDecimal.ZERO);
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.ZERO);
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.ZERO);
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.ZERO);

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertEquals(0, result.finalScore().compareTo(BigDecimal.valueOf(60)));
            assertEquals(HealthScoreStatus.NEEDS_ATTENTION, result.statusFromScore());
        }

        @Test
        void shouldTreatNullExpensesAsZeroWhenCalculatingSavingsScore() {
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(null, BigDecimal.valueOf(100));
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.ZERO);
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.highestExpenses(USER_ID, from, to, 5))
                    .thenReturn(List.of(new HighestExpenseDto(ExpenseCategory.FOOD, BigDecimal.valueOf(100))));

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertEquals(0, result.finalScore().compareTo(BigDecimal.valueOf(40)));
            assertEquals(HealthScoreStatus.POOR, result.statusFromScore());
        }

        @Test
        void shouldReturnProportionalSavingsScoreWhenSavingsRateBelowTarget() {
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(85), BigDecimal.valueOf(100));
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.ZERO);
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.highestExpenses(USER_ID, from, to, 5))
                    .thenReturn(List.of(new HighestExpenseDto(ExpenseCategory.FOOD, BigDecimal.valueOf(100))));

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertEquals(0, result.finalScore().compareTo(BigDecimal.valueOf(20)));
            assertEquals(HealthScoreStatus.POOR, result.statusFromScore());
        }

        @Test
        void shouldReturnFullBufferScoreWhenAvgExpensesIsZero() {
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(null);
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.valueOf(500));
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.ZERO);
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.highestExpenses(USER_ID, from, to, 5))
                    .thenReturn(List.of(new HighestExpenseDto(ExpenseCategory.FOOD, BigDecimal.valueOf(100))));

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertEquals(0, result.finalScore().compareTo(BigDecimal.valueOf(35)));
            assertEquals(HealthScoreStatus.POOR, result.statusFromScore());
        }

        @Test
        void shouldReturnFullBufferScoreWhenAvgExpensesIsNull() {
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(null);
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.valueOf(500));
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(null);
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.highestExpenses(USER_ID, from, to, 5))
                    .thenReturn(List.of(new HighestExpenseDto(ExpenseCategory.FOOD, BigDecimal.valueOf(100))));

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertEquals(0, result.finalScore().compareTo(BigDecimal.valueOf(35)));
            assertEquals(HealthScoreStatus.POOR, result.statusFromScore());
        }

        @Test
        void shouldTreatNullWalletBalanceAsZeroWhenCalculatingBufferScore() {
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(1000));
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.ZERO, BigDecimal.ZERO);
            when(reportClient.walletBalance(USER_ID)).thenReturn(null);
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertEquals(0, result.finalScore().compareTo(BigDecimal.valueOf(65)));
            assertEquals(HealthScoreStatus.NEEDS_ATTENTION, result.statusFromScore());
        }

        @Test
        void shouldReturnFullBufferScoreWhenMonthsCoveredExceedsTarget() {
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(null);
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.valueOf(1000));
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.highestExpenses(USER_ID, from, to, 5))
                    .thenReturn(List.of(new HighestExpenseDto(ExpenseCategory.FOOD, BigDecimal.valueOf(100))));

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertEquals(0, result.finalScore().compareTo(BigDecimal.valueOf(35)));
            assertEquals(HealthScoreStatus.POOR, result.statusFromScore());
        }

        @Test
        void shouldReturnProportionalBufferScoreWhenMonthsCoveredBelowTarget() {
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(null);
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.valueOf(150));
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.highestExpenses(USER_ID, from, to, 5))
                    .thenReturn(List.of(new HighestExpenseDto(ExpenseCategory.FOOD, BigDecimal.valueOf(100))));

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertEquals(0, result.finalScore().compareTo(BigDecimal.valueOf(18)));
            assertEquals(HealthScoreStatus.POOR, result.statusFromScore());
        }

        @Test
        void shouldReturnFullConcentrationScoreWhenTotalExpensesIsZero() {
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(null);
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.ZERO);
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.ZERO);

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertEquals(0, result.finalScore().compareTo(BigDecimal.valueOf(25)));
            assertEquals(HealthScoreStatus.POOR, result.statusFromScore());
        }

        @Test
        void shouldReturnFullConcentrationScoreWhenTotalExpensesIsNull() {
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(null);
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.ZERO);
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(null);

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertEquals(0, result.finalScore().compareTo(BigDecimal.valueOf(25)));
            assertEquals(HealthScoreStatus.POOR, result.statusFromScore());
        }

        @Test
        void shouldReturnFullConcentrationScoreWhenTopExpensesListIsEmpty() {
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(null);
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.ZERO);
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.highestExpenses(USER_ID, from, to, 5)).thenReturn(Collections.emptyList());

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertEquals(0, result.finalScore().compareTo(BigDecimal.valueOf(25)));
            assertEquals(HealthScoreStatus.POOR, result.statusFromScore());
        }

        @Test
        void shouldReturnFullConcentrationScoreWhenTopExpensesListIsNull() {
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(null);
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.ZERO);
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.highestExpenses(USER_ID, from, to, 5)).thenReturn(null);

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertEquals(0, result.finalScore().compareTo(BigDecimal.valueOf(25)));
            assertEquals(HealthScoreStatus.POOR, result.statusFromScore());
        }

        @Test
        void shouldIgnoreNullAmountsWhenSummingTopExpenses() {
            List<HighestExpenseDto> topExpenses = Arrays.asList(
                    new HighestExpenseDto(ExpenseCategory.FOOD, null),
                    new HighestExpenseDto(ExpenseCategory.FOOD, BigDecimal.valueOf(100)));
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(null);
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.ZERO);
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(1000));
            when(reportClient.highestExpenses(USER_ID, from, to, 5)).thenReturn(topExpenses);

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertEquals(0, result.finalScore().compareTo(BigDecimal.valueOf(23)));
            assertEquals(HealthScoreStatus.POOR, result.statusFromScore());
        }

        @Test
        void shouldClampConcentrationScoreToZeroWhenTopExpensesExceedTotalExpenses() {
            List<HighestExpenseDto> topExpenses = List.of(new HighestExpenseDto(ExpenseCategory.FOOD, BigDecimal.valueOf(150)));
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(null);
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.ZERO);
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.highestExpenses(USER_ID, from, to, 5)).thenReturn(topExpenses);

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertEquals(0, result.finalScore().compareTo(BigDecimal.ZERO));
            assertEquals(HealthScoreStatus.POOR, result.statusFromScore());
        }

        @Test
        void shouldReturnExcellentStatusAtLowerBoundaryScoreEightyFive() {
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(74.5), BigDecimal.valueOf(100));
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.valueOf(255));
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.highestExpenses(USER_ID, from, to, 5))
                    .thenReturn(List.of(new HighestExpenseDto(ExpenseCategory.FOOD, BigDecimal.valueOf(15))));

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertEquals(0, result.finalScore().compareTo(BigDecimal.valueOf(85)));
            assertEquals(HealthScoreStatus.EXCELLENT, result.statusFromScore());
        }

        @Test
        void shouldQueryReportClientWithFirstDayOfMonthAndTodayAndSampleSizeFive() {
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(1000));
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.ZERO, BigDecimal.valueOf(100));
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.valueOf(1000));
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.highestExpenses(USER_ID, from, to, 5)).thenReturn(Collections.emptyList());

            healthScoreService.getHealthScore(USER_ID);

            verify(reportClient, times(1)).sumRevenues(eq(USER_ID), eq(from), eq(to));
            verify(reportClient, times(1)).walletBalance(eq(USER_ID));
            verify(reportClient, times(1)).avgExpenses(eq(USER_ID), eq(from), eq(to));
            verify(reportClient, times(1)).highestExpenses(eq(USER_ID), eq(from), eq(to), eq(5));
        }

        @Test
        void shouldReturnGoodStatusAtLowerBoundaryScoreSeventy() {
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(79), BigDecimal.valueOf(100));
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.valueOf(210));
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.highestExpenses(USER_ID, from, to, 5))
                    .thenReturn(List.of(new HighestExpenseDto(ExpenseCategory.FOOD, BigDecimal.valueOf(30))));

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertEquals(0, result.finalScore().compareTo(BigDecimal.valueOf(70)));
            assertEquals(HealthScoreStatus.GOOD, result.statusFromScore());
        }

        @Test
        void shouldReturnNeedsAttentionStatusAtLowerBoundaryScoreFifty() {
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(85), BigDecimal.valueOf(100));
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.valueOf(150));
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.highestExpenses(USER_ID, from, to, 5))
                    .thenReturn(List.of(new HighestExpenseDto(ExpenseCategory.FOOD, BigDecimal.valueOf(50))));

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertEquals(0, result.finalScore().compareTo(BigDecimal.valueOf(50)));
            assertEquals(HealthScoreStatus.NEEDS_ATTENTION, result.statusFromScore());
        }

        @Test
        void shouldReturnGoodStatusJustBelowExcellentBoundary() {
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(74.8), BigDecimal.valueOf(100));
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.valueOf(252));
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.highestExpenses(USER_ID, from, to, 5))
                    .thenReturn(List.of(new HighestExpenseDto(ExpenseCategory.FOOD, BigDecimal.valueOf(16))));

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertEquals(0, result.finalScore().compareTo(BigDecimal.valueOf(84)));
            assertEquals(HealthScoreStatus.GOOD, result.statusFromScore());
        }

        @Test
        void shouldReturnPoorStatusWhenAllComponentScoresAreAtMinimum() {
            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(null);
            when(reportClient.walletBalance(USER_ID)).thenReturn(BigDecimal.ZERO);
            when(reportClient.avgExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.highestExpenses(USER_ID, from, to, 5))
                    .thenReturn(List.of(new HighestExpenseDto(ExpenseCategory.FOOD, BigDecimal.valueOf(100))));

            HealthScoreDto result = healthScoreService.getHealthScore(USER_ID);

            assertEquals(0, result.finalScore().compareTo(BigDecimal.ZERO));
            assertEquals(HealthScoreStatus.POOR, result.statusFromScore());
        }
    }
}