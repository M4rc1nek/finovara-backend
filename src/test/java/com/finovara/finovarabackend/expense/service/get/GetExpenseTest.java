package com.finovara.finovarabackend.expense.service.get;

import com.finovara.finovarabackend.expense.dto.ExpenseDTO;
import com.finovara.finovarabackend.expense.mapper.ExpenseMapper;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.expense.service.ExpenseService;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetExpenseTest {

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private UserManagerService userManagerService;
    @Mock
    private ExpenseMapper expenseMapper;

    @InjectMocks
    private ExpenseService expenseService;

    @Test
    void shouldReturnExpensesForUser() {

        String email = "test@email.com";

        User user = new User();
        user.setId(1L);

        Expense expense1 = new Expense();
        Expense expense2 = new Expense();

        ExpenseDTO dto = new ExpenseDTO(null, null, new BigDecimal("100"), ExpenseCategory.FOOD, null, "food");

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(expenseRepository.findAllByUserAssignedId(user.getId())).thenReturn(List.of(expense1, expense2));

        when(expenseMapper.mapExpenseToDTO(any(Expense.class))).thenReturn(dto);

        List<ExpenseDTO> result = expenseService.getExpense(email);

        assertEquals(2, result.size());

        verify(userManagerService).getUserByEmailOrThrow(email);
        verify(expenseRepository).findAllByUserAssignedId(user.getId());
        verify(expenseMapper, times(2)).mapExpenseToDTO(any(Expense.class));
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoExpenses() {

        String email = "test@email.com";

        User user = new User();
        user.setId(1L);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(expenseRepository.findAllByUserAssignedId(user.getId())).thenReturn(List.of());

        List<ExpenseDTO> result = expenseService.getExpense(email);

        assertTrue(result.isEmpty());

        verify(expenseRepository).findAllByUserAssignedId(user.getId());
        verify(expenseMapper, never()).mapExpenseToDTO(any());
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {

        String email = "test@email.com";

        when(userManagerService.getUserByEmailOrThrow(email)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> expenseService.getExpense(email));

        verify(expenseRepository, never()).findAllByUserAssignedId(any());
    }
}

