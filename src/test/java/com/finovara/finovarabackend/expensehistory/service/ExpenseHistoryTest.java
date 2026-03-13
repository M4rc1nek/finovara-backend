package com.finovara.finovarabackend.expensehistory.service;

import com.finovara.finovarabackend.expense.dto.ExpenseDTO;
import com.finovara.finovarabackend.expense.mapper.ExpenseMapper;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.time.SpentInPeriodService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExpenseHistoryTest {

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private SpentInPeriodService spentInPeriodService;
    @Mock
    private UserManagerService userManagerService;
    @Mock
    private ExpenseMapper expenseMapper;

    @InjectMocks
    private ExpenseHistoryService expenseHistoryService;

    @Test
    void shouldGetExpenseHistoryByCategorySuccessfully() {
        String email = "test@email.com";
        User user = new User();
        user.setId(1L);

        Expense expense = new Expense();
        expense.setId(1L);
        Expense expense2 = new Expense();
        expense2.setId(2L);

        LocalDate today = LocalDate.of(2025, 12, 5);
        LocalDate startMonth = today.withDayOfMonth(1);

        ExpenseDTO dto = new ExpenseDTO(null, null, new BigDecimal("100"), ExpenseCategory.FOOD, today, "test");

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(spentInPeriodService.today()).thenReturn(today);
        when(expenseRepository.findAllByUserAndCategoryAndDateRange(user.getId(), ExpenseCategory.FOOD, startMonth, today))
                .thenReturn(List.of(expense, expense2));
        when(expenseMapper.mapExpenseToDTO(any(Expense.class))).thenReturn(dto);

        List<ExpenseDTO> result = expenseHistoryService.getExpenseByCategory(email, ExpenseCategory.FOOD);

        assertEquals(2, result.size());
    }

    @Test
    void shouldReturnEmptyListWhenNoExpensesFound() {
        String email = "empty@email.com";
        User user = new User();
        user.setId(2L);

        LocalDate today = LocalDate.of(2025, 12, 5);
        LocalDate startMonth = today.withDayOfMonth(1);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(spentInPeriodService.today()).thenReturn(today);
        when(expenseRepository.findAllByUserAndCategoryAndDateRange(user.getId(), ExpenseCategory.FOOD, startMonth, today)).thenReturn(List.of());

        List<ExpenseDTO> result = expenseHistoryService.getExpenseByCategory(email, ExpenseCategory.FOOD);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenUserNotFound() {
        String email = "notfound@email.com";

        when(userManagerService.getUserByEmailOrThrow(email)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> expenseHistoryService.getExpenseByCategory(email, ExpenseCategory.FOOD));
    }
}