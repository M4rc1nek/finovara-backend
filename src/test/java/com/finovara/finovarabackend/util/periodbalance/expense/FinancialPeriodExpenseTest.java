package com.finovara.finovarabackend.util.periodbalance.expense;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialPeriodExpenseTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private FinancialPeriodService financialPeriodService;

    private final Long USER_ID = 1L;
    LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnExpensesInPeriod(PeriodType periodType) {
        LocalDate from = periodType.getStartDate(today);

        when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, from, today))
                .thenReturn(Optional.of(BigDecimal.valueOf(50)));

        BigDecimal result = financialPeriodService.getExpensesSum(USER_ID, periodType);

        assertThat(result).isEqualByComparingTo("50");
        verify(expenseRepository).sumExpensesByUserAndDateRange(USER_ID, from, today);
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnZeroWhenNoExpenses(PeriodType periodType) {
        LocalDate from = periodType.getStartDate(today);

        when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, from, today)).thenReturn(Optional.empty());

        BigDecimal result = financialPeriodService.getExpensesSum(USER_ID, periodType);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnExpensesInPeriodByCategory(PeriodType periodType) {
        LocalDate from = periodType.getStartDate(today);

        financialPeriodService.getExpensesInPeriodByCategory(USER_ID, periodType, ExpenseCategory.FOOD);

        verify(expenseRepository).findAllByUserAssignedIdAndCreatedAtBetweenAndCategory(USER_ID, from, today, ExpenseCategory.FOOD);
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnAverageExpenseInPeriod(PeriodType periodType) {
        LocalDate from = periodType.getStartDate(today);

        when(expenseRepository.avgExpensesByUserAssignedIdAndPeriod(USER_ID, from, today))
                .thenReturn(Optional.of(BigDecimal.valueOf(75)));

        BigDecimal result = financialPeriodService.getAverageExpense(USER_ID, periodType);

        assertThat(result).isEqualByComparingTo("75");
        verify(expenseRepository).avgExpensesByUserAssignedIdAndPeriod(USER_ID, from, today);
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnZeroAverageExpenseWhenNoData(PeriodType periodType) {
        LocalDate from = periodType.getStartDate(today);

        when(expenseRepository.avgExpensesByUserAssignedIdAndPeriod(USER_ID, from, today))
                .thenReturn(Optional.empty());

        BigDecimal result = financialPeriodService.getAverageExpense(USER_ID, periodType);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

}
