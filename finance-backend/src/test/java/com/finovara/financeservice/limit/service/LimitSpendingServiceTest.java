package com.finovara.financeservice.limit.service;

import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.internal.report.util.dto.ExpenseCategorySumDto;
import com.finovara.financeservice.limit.model.Limit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LimitSpendingServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long GENERAL_LIMIT_ID = 10L;
    private static final Long CATEGORY_LIMIT_ID = 11L;
    private static final Long OTHER_PERIOD_LIMIT_ID = 12L;
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private static final PeriodType PERIOD = PeriodType.values()[0];
    private static final PeriodType OTHER_PERIOD = PeriodType.values()[1];
    private static final ExpenseCategory CATEGORY = ExpenseCategory.values()[0];
    private static final ExpenseCategory OTHER_CATEGORY = ExpenseCategory.values()[1];
    private static final BigDecimal LIMIT_AMOUNT = BigDecimal.valueOf(1000);

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private LimitSpendingService service;

    private Limit generalLimit;
    private Limit categoryLimit;
    private Limit otherPeriodLimit;

    @BeforeEach
    void setUp() {
        generalLimit = Limit.builder().id(GENERAL_LIMIT_ID).periodType(PERIOD).category(null).amount(LIMIT_AMOUNT).build();
        categoryLimit = Limit.builder().id(CATEGORY_LIMIT_ID).periodType(PERIOD).category(CATEGORY).amount(LIMIT_AMOUNT).build();
        otherPeriodLimit = Limit.builder().id(OTHER_PERIOD_LIMIT_ID).periodType(OTHER_PERIOD).category(null).amount(LIMIT_AMOUNT).build();
    }

    @Nested
    class CalculateSpent {

        @Test
        void shouldSumAllCategoriesWhenGeneralLimit() {
            when(expenseRepository.sumExpensesGroupedByCategory(USER_ID, PERIOD.getStartDate(TODAY), TODAY))
                    .thenReturn(List.of(
                            new ExpenseCategorySumDto(CATEGORY, BigDecimal.valueOf(10)),
                            new ExpenseCategorySumDto(OTHER_CATEGORY, BigDecimal.valueOf(20))));

            Map<Long, BigDecimal> result = service.calculateSpent(USER_ID, List.of(generalLimit), TODAY);

            assertEquals(BigDecimal.valueOf(30), result.get(GENERAL_LIMIT_ID));
        }

        @Test
        void shouldSumOnlyMatchingCategoryWhenCategoryLimit() {
            when(expenseRepository.sumExpensesGroupedByCategory(USER_ID, PERIOD.getStartDate(TODAY), TODAY))
                    .thenReturn(List.of(
                            new ExpenseCategorySumDto(CATEGORY, BigDecimal.valueOf(10)),
                            new ExpenseCategorySumDto(OTHER_CATEGORY, BigDecimal.valueOf(20))));

            Map<Long, BigDecimal> result = service.calculateSpent(USER_ID, List.of(categoryLimit), TODAY);

            assertEquals(BigDecimal.valueOf(10), result.get(CATEGORY_LIMIT_ID));
        }

        @Test
        void shouldReturnZeroWhenNoCategoryMatchesLimit() {
            when(expenseRepository.sumExpensesGroupedByCategory(USER_ID, PERIOD.getStartDate(TODAY), TODAY))
                    .thenReturn(List.of(new ExpenseCategorySumDto(OTHER_CATEGORY, BigDecimal.valueOf(20))));

            Map<Long, BigDecimal> result = service.calculateSpent(USER_ID, List.of(categoryLimit), TODAY);

            assertEquals(BigDecimal.ZERO, result.get(CATEGORY_LIMIT_ID));
        }

        @Test
        void shouldReturnZeroWhenNoExpensesFound() {
            when(expenseRepository.sumExpensesGroupedByCategory(USER_ID, PERIOD.getStartDate(TODAY), TODAY))
                    .thenReturn(List.of());

            Map<Long, BigDecimal> result = service.calculateSpent(USER_ID, List.of(generalLimit), TODAY);

            assertEquals(BigDecimal.ZERO, result.get(GENERAL_LIMIT_ID));
        }

        @Test
        void shouldSumMultipleEntriesOfSameCategoryWhenCategoryLimit() {
            when(expenseRepository.sumExpensesGroupedByCategory(USER_ID, PERIOD.getStartDate(TODAY), TODAY))
                    .thenReturn(List.of(
                            new ExpenseCategorySumDto(CATEGORY, BigDecimal.valueOf(10)),
                            new ExpenseCategorySumDto(CATEGORY, BigDecimal.valueOf(15))));

            Map<Long, BigDecimal> result = service.calculateSpent(USER_ID, List.of(categoryLimit), TODAY);

            assertEquals(BigDecimal.valueOf(25), result.get(CATEGORY_LIMIT_ID));
        }

        @Test
        void shouldQueryRepositoryOnceWhenLimitsShareSamePeriod() {
            when(expenseRepository.sumExpensesGroupedByCategory(USER_ID, PERIOD.getStartDate(TODAY), TODAY))
                    .thenReturn(List.of(
                            new ExpenseCategorySumDto(CATEGORY, BigDecimal.valueOf(10)),
                            new ExpenseCategorySumDto(OTHER_CATEGORY, BigDecimal.valueOf(20))));

            Map<Long, BigDecimal> result = service.calculateSpent(USER_ID, List.of(generalLimit, categoryLimit), TODAY);

            assertEquals(BigDecimal.valueOf(30), result.get(GENERAL_LIMIT_ID));
            assertEquals(BigDecimal.valueOf(10), result.get(CATEGORY_LIMIT_ID));
            verify(expenseRepository, times(1)).sumExpensesGroupedByCategory(USER_ID, PERIOD.getStartDate(TODAY), TODAY);
        }

        @Test
        void shouldQueryRepositoryForEachPeriodWhenLimitsHaveDifferentPeriods() {
            when(expenseRepository.sumExpensesGroupedByCategory(USER_ID, PERIOD.getStartDate(TODAY), TODAY))
                    .thenReturn(List.of(new ExpenseCategorySumDto(CATEGORY, BigDecimal.valueOf(10))));
            when(expenseRepository.sumExpensesGroupedByCategory(USER_ID, OTHER_PERIOD.getStartDate(TODAY), TODAY))
                    .thenReturn(List.of(new ExpenseCategorySumDto(CATEGORY, BigDecimal.valueOf(40))));

            Map<Long, BigDecimal> result = service.calculateSpent(USER_ID, List.of(generalLimit, otherPeriodLimit), TODAY);

            assertEquals(BigDecimal.valueOf(10), result.get(GENERAL_LIMIT_ID));
            assertEquals(BigDecimal.valueOf(40), result.get(OTHER_PERIOD_LIMIT_ID));
            verify(expenseRepository).sumExpensesGroupedByCategory(USER_ID, PERIOD.getStartDate(TODAY), TODAY);
            verify(expenseRepository).sumExpensesGroupedByCategory(USER_ID, OTHER_PERIOD.getStartDate(TODAY), TODAY);
        }

        @Test
        void shouldReturnEmptyMapWhenNoLimitsProvided() {
            Map<Long, BigDecimal> result = service.calculateSpent(USER_ID, List.of(), TODAY);

            assertTrue(result.isEmpty());
            verifyNoInteractions(expenseRepository);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(expenseRepository.sumExpensesGroupedByCategory(USER_ID, PERIOD.getStartDate(TODAY), TODAY))
                    .thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> service.calculateSpent(USER_ID, List.of(generalLimit), TODAY));
        }
    }
}