package com.finovara.finovarabackend.reports.service.highest;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.reports.dto.ReportsHighestExpense;
import com.finovara.finovarabackend.reports.service.ReportsService;
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
class ReportsHighestExpenseTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ReportsService reportsService;

    private final Long USER_ID = 1L;

    @Test
    void shouldReturnTop3HighestExpenses() {

        int year = 2025;
        int month = 3;

        Expense expense1 = new Expense();
        expense1.setAmount(BigDecimal.valueOf(100));
        expense1.setCategory(ExpenseCategory.FOOD);

        Expense expense2 = new Expense();
        expense2.setAmount(BigDecimal.valueOf(300));
        expense2.setCategory(ExpenseCategory.TRANSPORT);

        Expense expense3 = new Expense();
        expense3.setAmount(BigDecimal.valueOf(200));
        expense3.setCategory(ExpenseCategory.ENTERTAINMENT);

        Expense expense4 = new Expense();
        expense4.setAmount(BigDecimal.valueOf(50));
        expense4.setCategory(ExpenseCategory.HEALTH);

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        when(expenseRepository.findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, from, to)).thenReturn(List.of(expense1, expense2, expense3, expense4));

        List<ReportsHighestExpense> highestExpenses = reportsService.getHighestExpense(USER_ID, year, month);

        assertThat(highestExpenses).hasSize(3);

        assertThat(highestExpenses.get(0).expenseCategory()).isEqualTo(ExpenseCategory.TRANSPORT);
        assertThat(highestExpenses.get(0).amount()).isEqualByComparingTo(BigDecimal.valueOf(300));

        assertThat(highestExpenses.get(1).expenseCategory()).isEqualTo(ExpenseCategory.ENTERTAINMENT);
        assertThat(highestExpenses.get(1).amount()).isEqualByComparingTo(BigDecimal.valueOf(200));

        assertThat(highestExpenses.get(2).expenseCategory()).isEqualTo(ExpenseCategory.FOOD);
        assertThat(highestExpenses.get(2).amount()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void shouldReturnAllExpensesIfLessThan3() {

        int year = 2025;
        int month = 3;

        Expense expense1 = new Expense();
        expense1.setAmount(BigDecimal.valueOf(150));
        expense1.setCategory(ExpenseCategory.FOOD);

        Expense expense2 = new Expense();
        expense2.setAmount(BigDecimal.valueOf(50));
        expense2.setCategory(ExpenseCategory.TRANSPORT);

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        when(expenseRepository.findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, from, to)).thenReturn(List.of(expense1, expense2));

        List<ReportsHighestExpense> highestExpenses = reportsService.getHighestExpense(USER_ID, year, month);

        assertThat(highestExpenses).hasSize(2);

        assertThat(highestExpenses.get(0).expenseCategory()).isEqualTo(ExpenseCategory.FOOD);
        assertThat(highestExpenses.get(0).amount()).isEqualByComparingTo(BigDecimal.valueOf(150));

        assertThat(highestExpenses.get(1).expenseCategory()).isEqualTo(ExpenseCategory.TRANSPORT);
        assertThat(highestExpenses.get(1).amount()).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    void shouldReturnEmptyListIfNoExpenses() {

        int year = 2025;
        int month = 3;

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        when(expenseRepository.findAllByUserAssignedIdAndCreatedAtBetween(USER_ID, from, to))
                .thenReturn(List.of());

        List<ReportsHighestExpense> highestExpenses = reportsService.getHighestExpense(USER_ID, year, month);

        assertThat(highestExpenses).isEmpty();
    }
}