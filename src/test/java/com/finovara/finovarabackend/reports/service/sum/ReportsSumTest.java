package com.finovara.finovarabackend.reports.service.sum;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.reports.finances.dto.ReportsSumDTO;
import com.finovara.finovarabackend.reports.finances.service.ReportsService;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportsSumTest  {

    @Mock
    private RevenueRepository revenueRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ReportsService reportsService;

    private final Long USER_ID = 1L;

    @Test
    void shouldSumRevenueAndExpenseForMonth() {

        int year = 2025;
        int month = 3;

        Revenue revenue = new Revenue();
        revenue.setAmount(BigDecimal.valueOf(100));

        Revenue revenue2 = new Revenue();
        revenue2.setAmount(BigDecimal.valueOf(200));

        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(50));

        Expense expense2 = new Expense();
        expense2.setAmount(BigDecimal.valueOf(70));

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        when(revenueRepository.findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, from, to)).thenReturn(List.of(revenue, revenue2));

        when(expenseRepository.findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, from, to)).thenReturn(List.of(expense, expense2));

        ReportsSumDTO result = reportsService.sumRevenueAndExpense(USER_ID, year, month);

        assertThat(result.sumRevenue()).isEqualByComparingTo("300");
        assertThat(result.sumExpense()).isEqualByComparingTo("120");
    }

    @Test
    void shouldReturnZeroWhenNoRevenueAndExpense() {

        int year = 2025;
        int month = 3;

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        when(revenueRepository.findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, from, to)).thenReturn(List.of());

        when(expenseRepository.findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, from, to)).thenReturn(List.of());

        ReportsSumDTO result = reportsService.sumRevenueAndExpense(USER_ID, year, month);

        assertThat(result.sumRevenue()).isEqualByComparingTo("0");
        assertThat(result.sumExpense()).isEqualByComparingTo("0");
    }

    @Test
    void shouldReturnRevenueWhenNoExpenses() {

        int year = 2025;
        int month = 3;

        Revenue revenue = new Revenue();
        revenue.setAmount(BigDecimal.valueOf(500));

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        when(revenueRepository.findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, from, to)).thenReturn(List.of(revenue));

        when(expenseRepository.findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, from, to)).thenReturn(List.of());

        ReportsSumDTO result = reportsService.sumRevenueAndExpense(USER_ID, year, month);

        assertThat(result.sumRevenue()).isEqualByComparingTo("500");
        assertThat(result.sumExpense()).isEqualByComparingTo("0");
    }
}