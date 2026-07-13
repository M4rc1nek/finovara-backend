package com.finovara.financeservice.util.expense;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
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
class SharedExpenseManagerServiceTest {

    @Mock
    private SharedExpenseRepository sharedExpenseRepository;

    @InjectMocks
    private SharedExpenseManagerService sharedExpenseManagerService;

    private final Long EXPENSE_ID = 1L;

    @Test
    void shouldReturnExpenseById() {
        SharedExpense expense = new SharedExpense();

        when(sharedExpenseRepository.findById(EXPENSE_ID)).thenReturn(Optional.of(expense));

        sharedExpenseManagerService.getSharedExpenseOrThrow(EXPENSE_ID);

        verify(sharedExpenseRepository).findById(EXPENSE_ID);
    }

    @Test
    void shouldThrowExceptionWhenExpenseByIdNotFound() {
        when(sharedExpenseRepository.findById(EXPENSE_ID)).thenReturn(Optional.empty());

        assertThrows(RequestedEntityNotFoundException.class, () -> sharedExpenseManagerService.getSharedExpenseOrThrow(EXPENSE_ID));

        verify(sharedExpenseRepository).findById(EXPENSE_ID);
    }

}