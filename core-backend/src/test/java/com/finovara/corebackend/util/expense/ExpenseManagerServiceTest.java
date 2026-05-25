package com.finovara.corebackend.util.expense;

import com.finovara.corebackend.expense.exception.notfound.ExpenseNotFoundException;
import com.finovara.corebackend.expense.model.Expense;
import com.finovara.corebackend.expense.repository.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseManagerServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ExpenseManagerService expenseManagerService;

    private final Long EXPENSE_ID = 1L;
    private final Long USER_ID = 2L;

    @Test
    void shouldReturnExpenseByUserIdAndExpenseId() {
        Expense expense = new Expense();

        when(expenseRepository.findByIdAndUserAssignedId(EXPENSE_ID, USER_ID)).thenReturn(Optional.of(expense));

        expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID);

        verify(expenseRepository).findByIdAndUserAssignedId(EXPENSE_ID, USER_ID);
    }

    @Test
    void shouldThrowWhenExpenseByUserIdNotFound() {
        when(expenseRepository.findByIdAndUserAssignedId(EXPENSE_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(ExpenseNotFoundException.class, () -> expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, USER_ID));

        verify(expenseRepository).findByIdAndUserAssignedId(EXPENSE_ID, USER_ID);
    }

    @Test
    void shouldReturnExpenseById() {
        Expense expense = new Expense();

        when(expenseRepository.findById(EXPENSE_ID)).thenReturn(Optional.of(expense));

        expenseManagerService.getExpenseByIdOrThrow(EXPENSE_ID);

        verify(expenseRepository).findById(EXPENSE_ID);
    }

    @Test
    void shouldThrowWhenExpenseByIdNotFound() {
        when(expenseRepository.findById(EXPENSE_ID)).thenReturn(Optional.empty());

        assertThrows(ExpenseNotFoundException.class, () -> expenseManagerService.getExpenseByIdOrThrow(EXPENSE_ID));

        verify(expenseRepository).findById(EXPENSE_ID);
    }
}