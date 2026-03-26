package com.finovara.finovarabackend.report.finances.service.average;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.finances.dto.ReportsAverageDTO;
import com.finovara.finovarabackend.report.finances.service.ReportsService;
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
class ReportsAverageTest {

    @Mock
    private RevenueRepository revenueRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ReportsService reportsService;

    private final Long USER_ID = 1L;

    @Test
    void shouldCalculateAverageRevenueAndExpense() {

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

        ReportsAverageDTO result = reportsService.calculateAverageRevenueAndExpense(USER_ID, year, month);

        assertThat(result.averageRevenue()).isEqualByComparingTo("150.00");
        assertThat(result.averageExpense()).isEqualByComparingTo("60.00");
    }

    @Test
    void shouldReturnZeroWhenNoRevenueAndExpense() {

        int year = 2025;
        int month = 3;

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        when(revenueRepository.findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, from, to)).thenReturn(List.of());
        when(expenseRepository.findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, from, to)).thenReturn(List.of());

        ReportsAverageDTO result = reportsService.calculateAverageRevenueAndExpense(USER_ID, year, month);

        assertThat(result.averageRevenue()).isEqualByComparingTo("0");
        assertThat(result.averageExpense()).isEqualByComparingTo("0");
    }

    @Test
    void shouldReturnAverageExpenseWhenNoRevenue() {

        int year = 2025;
        int month = 3;

        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(30));

        Expense expense2 = new Expense();
        expense2.setAmount(BigDecimal.valueOf(70));

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        when(revenueRepository.findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, from, to)).thenReturn(List.of());
        when(expenseRepository.findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, from, to)).thenReturn(List.of(expense, expense2));

        ReportsAverageDTO result = reportsService.calculateAverageRevenueAndExpense(USER_ID, year, month);

        assertThat(result.averageRevenue()).isEqualByComparingTo("0");
        assertThat(result.averageExpense()).isEqualByComparingTo("50.00");
    }

    @Test
    void shouldReturnAverageRevenueWhenNoExpenses() {

        int year = 2025;
        int month = 3;

        Revenue revenue = new Revenue();
        revenue.setAmount(BigDecimal.valueOf(400));

        Revenue revenue2 = new Revenue();
        revenue2.setAmount(BigDecimal.valueOf(200));

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        when(revenueRepository.findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, from, to)).thenReturn(List.of(revenue, revenue2));
        when(expenseRepository.findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, from, to)).thenReturn(List.of());

        ReportsAverageDTO result = reportsService.calculateAverageRevenueAndExpense(USER_ID, year, month);

        assertThat(result.averageRevenue()).isEqualByComparingTo("300.00");
        assertThat(result.averageExpense()).isEqualByComparingTo("0");
    }
}