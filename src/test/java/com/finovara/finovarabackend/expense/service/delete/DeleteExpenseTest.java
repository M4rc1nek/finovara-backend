package com.finovara.finovarabackend.expense.service.delete;

import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivityType;
import com.finovara.finovarabackend.accountactivity.expense.service.ExpenseActivityService;
import com.finovara.finovarabackend.expense.exception.notfound.ExpenseNotFoundException;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.expense.service.ExpenseService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.finovarabackend.usersetting.piggybank.roundup.service.RoundUpService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import com.finovara.finovarabackend.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteExpenseTest {
    @Mock
    private UserManagerService userManagerService;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private WalletService walletService;
    @Mock
    private ExpenseActivityService expenseActivityService;
    @Mock
    private RoundUpService roundUpService;

    @InjectMocks
    private ExpenseService expenseService;

    @Test
    void shouldDeleteExpenseSuccessfully() {
        String email = "test@mail.com";

        User user = new User();
        user.setId(1L);

        Expense expense = new Expense();
        expense.setId(1L);
        expense.setUserAssigned(user);
        expense.setAmount(new BigDecimal("100"));

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(expenseRepository.findByIdAndUserAssignedId(expense.getId(), user.getId())).thenReturn(Optional.of(expense));

        expenseService.deleteExpense(expense.getId(), email);

        InOrder inOrder = inOrder(roundUpService, walletService, expenseActivityService, expenseRepository);
        inOrder.verify(roundUpService).handleExpenseForRoundUp(email, expense.getId(), PiggyBankAutomationMode.ROLLBACK);
        inOrder.verify(walletService).addBalanceToWallet(email, new BigDecimal("100"));
        inOrder.verify(expenseActivityService).createExpenseActivity(email, ExpenseActivityType.DELETED_EXPENSE, expense);
        inOrder.verify(expenseRepository).delete(expense);

        verify(userManagerService).getUserByEmailOrThrow(email);
        verify(expenseRepository).findByIdAndUserAssignedId(expense.getId(), user.getId());
        verifyNoMoreInteractions(roundUpService, walletService, expenseActivityService, expenseRepository);
    }

    @Test
    void shouldThrowWhenExpenseDoesNotExist() {
        String email = "test@gmail.com";

        User user = new User();
        user.setId(1L);

        Long expenseId = 1L;

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(expenseRepository.findByIdAndUserAssignedId(expenseId, user.getId())).thenReturn(Optional.empty());

        assertThrows(ExpenseNotFoundException.class, () -> expenseService.deleteExpense(expenseId, email));

        verify(userManagerService).getUserByEmailOrThrow(email);
        verify(expenseRepository).findByIdAndUserAssignedId(expenseId, user.getId());
        verify(expenseRepository, never()).delete(any());
        verifyNoInteractions(roundUpService, walletService, expenseActivityService);

    }

}