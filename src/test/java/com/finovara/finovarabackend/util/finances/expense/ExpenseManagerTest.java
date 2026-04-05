package com.finovara.finovarabackend.util.finances.expense;

import com.finovara.finovarabackend.expense.exception.notfound.ExpenseNotFoundException;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.util.expense.ExpenseManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseManagerTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ExpenseManagerService expenseManagerService;

    @Test
    void shouldReturnExpenseWhenExpenseIdAndUserIdExist() {
        Expense expense = new Expense();
        expense.setId(1L);

        when(expenseRepository.findByIdAndUserAssignedId(1L, 100L)).thenReturn(Optional.of(expense));

        Expense result = expenseManagerService.getExpenseByUserIdOrThrow(1L, 100L);

        assertEquals(1L, result.getId());
    }

    @Test
    void shouldThrowExpenseNotFoundExceptionWhenExpenseIdAndUserIdDoNotExist() {
        when(expenseRepository.findByIdAndUserAssignedId(1L, 100L)).thenReturn(Optional.empty());

        assertThrows(ExpenseNotFoundException.class, () -> expenseManagerService.getExpenseByUserIdOrThrow(1L, 100L));
    }

    @Test
    void shouldReturnExpenseWhenExpenseIdExists() {
        Expense expense = new Expense();
        expense.setId(1L);

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        Expense result = expenseManagerService.getExpenseByIdOrThrow(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void shouldThrowExpenseNotFoundExceptionWhenExpenseIdDoesNotExist() {
        when(expenseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ExpenseNotFoundException.class, () -> expenseManagerService.getExpenseByIdOrThrow(1L));
    }
}