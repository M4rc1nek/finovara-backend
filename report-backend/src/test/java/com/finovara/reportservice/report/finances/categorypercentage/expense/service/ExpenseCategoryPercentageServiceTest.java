package com.finovara.reportservice.report.finances.categorypercentage.expense.service;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.report.finances.categorypercentage.expense.dto.ExpenseCategoryPercentageDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseCategoryPercentageServiceTest {

    private static final Long USER_ID = 1L;
    private static final ExpenseCategory CATEGORY = ExpenseCategory.CLOTHING;

    @Mock
    private FinanceBackendReportClient reportClient;

    @InjectMocks
    private ExpenseCategoryPercentageService expenseCategoryPercentageService;

    @Nested
    class GetExpensePercentageByCategoryReport {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldDelegateToClientAndReturnPercentage(PeriodType periodType) {
            LocalDate to = LocalDate.now();
            LocalDate from = periodType.getStartDate(to);

            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.expensesByCategory(USER_ID, from, to, CATEGORY)).thenReturn(BigDecimal.valueOf(50));

            ExpenseCategoryPercentageDto result = expenseCategoryPercentageService
                    .getExpensePercentageByCategoryReport(USER_ID, CATEGORY, periodType);

            assertThat(result.percentage()).isEqualByComparingTo("50");
            assertThat(result.category()).isEqualTo(CATEGORY);
            verify(reportClient).sumExpenses(USER_ID, from, to);
            verify(reportClient).expensesByCategory(USER_ID, from, to, CATEGORY);
        }

        @Test
        void shouldReturnZeroPercentageWhenTotalOrCategoryAmountIsZero() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.MONTHLY.getStartDate(to);

            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.expensesByCategory(USER_ID, from, to, CATEGORY)).thenReturn(BigDecimal.ZERO);

            ExpenseCategoryPercentageDto result = expenseCategoryPercentageService
                    .getExpensePercentageByCategoryReport(USER_ID, CATEGORY, PeriodType.MONTHLY);

            assertThat(result.percentage()).isEqualByComparingTo("0");
        }

        @Test
        void shouldReturnOneHundredPercentWhenAllExpensesAreInCategory() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.MONTHLY.getStartDate(to);

            when(reportClient.sumExpenses(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.expensesByCategory(USER_ID, from, to, CATEGORY)).thenReturn(BigDecimal.valueOf(100));

            ExpenseCategoryPercentageDto result = expenseCategoryPercentageService
                    .getExpensePercentageByCategoryReport(USER_ID, CATEGORY, PeriodType.MONTHLY);

            assertThat(result.percentage()).isEqualByComparingTo("100");
        }
    }
}
