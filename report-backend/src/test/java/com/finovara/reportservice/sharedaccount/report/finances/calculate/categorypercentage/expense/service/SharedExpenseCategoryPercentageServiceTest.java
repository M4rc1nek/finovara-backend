package com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.expense.service;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.reportservice.feignclient.FinanceBackendSharedReportClient;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.expense.dto.SharedExpenseCategoryPercentageDto;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedExpenseCategoryPercentageServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;

    @Mock
    private FinanceBackendSharedReportClient reportClient;

    @Mock
    private ExpenseCategory expenseCategory;

    @InjectMocks
    private SharedExpenseCategoryPercentageService sharedExpenseCategoryPercentageService;

    @Nested
    class GetExpensePercentageByCategoryReport {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnCorrectPercentageForEveryPeriodType(PeriodType periodType) {
            LocalDate to = LocalDate.now();
            LocalDate from = periodType.getStartDate(to);
            when(reportClient.sumExpenses(OWNER_ID, MEMBER_ID, from, to)).thenReturn(new BigDecimal("400.00"));
            when(reportClient.expensesByCategory(OWNER_ID, MEMBER_ID, from, to, expenseCategory)).thenReturn(new BigDecimal("100.00"));

            SharedExpenseCategoryPercentageDto result = sharedExpenseCategoryPercentageService
                    .getExpensePercentageByCategoryReport(OWNER_ID, MEMBER_ID, expenseCategory, periodType);

            assertThat(result.percentage()).isEqualByComparingTo("25.00");
            assertThat(result.category()).isEqualTo(expenseCategory);
        }

        @Test
        void shouldReturnZeroPercentageWhenCategoryHasNoExpense() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.MONTHLY.getStartDate(to);
            when(reportClient.sumExpenses(OWNER_ID, MEMBER_ID, from, to)).thenReturn(new BigDecimal("400.00"));
            when(reportClient.expensesByCategory(OWNER_ID, MEMBER_ID, from, to, expenseCategory)).thenReturn(BigDecimal.ZERO);

            SharedExpenseCategoryPercentageDto result = sharedExpenseCategoryPercentageService
                    .getExpensePercentageByCategoryReport(OWNER_ID, MEMBER_ID, expenseCategory, PeriodType.MONTHLY);

            assertThat(result.percentage()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldReturnFullPercentageWhenCategoryEqualsTotalExpense() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.MONTHLY.getStartDate(to);
            when(reportClient.sumExpenses(OWNER_ID, MEMBER_ID, from, to)).thenReturn(new BigDecimal("60.00"));
            when(reportClient.expensesByCategory(OWNER_ID, MEMBER_ID, from, to, expenseCategory)).thenReturn(new BigDecimal("60.00"));

            SharedExpenseCategoryPercentageDto result = sharedExpenseCategoryPercentageService
                    .getExpensePercentageByCategoryReport(OWNER_ID, MEMBER_ID, expenseCategory, PeriodType.MONTHLY);

            assertThat(result.percentage()).isEqualByComparingTo("100.00");
        }

        @Test
        void shouldCallReportClientWithOwnerAndMemberIds() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.WEEKLY.getStartDate(to);
            when(reportClient.sumExpenses(OWNER_ID, MEMBER_ID, from, to)).thenReturn(new BigDecimal("10.00"));
            when(reportClient.expensesByCategory(OWNER_ID, MEMBER_ID, from, to, expenseCategory)).thenReturn(new BigDecimal("5.00"));

            sharedExpenseCategoryPercentageService.getExpensePercentageByCategoryReport(OWNER_ID, MEMBER_ID, expenseCategory, PeriodType.WEEKLY);

            verify(reportClient).sumExpenses(OWNER_ID, MEMBER_ID, from, to);
            verify(reportClient).expensesByCategory(OWNER_ID, MEMBER_ID, from, to, expenseCategory);
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            assertThrows(NullPointerException.class,
                    () -> sharedExpenseCategoryPercentageService.getExpensePercentageByCategoryReport(OWNER_ID, MEMBER_ID, expenseCategory, null));
        }
    }
}