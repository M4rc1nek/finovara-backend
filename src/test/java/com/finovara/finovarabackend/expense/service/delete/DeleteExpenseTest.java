package com.finovara.finovarabackend.expense.service.delete;

import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivityType;
import com.finovara.finovarabackend.accountactivity.expense.service.ExpenseActivityService;
import com.finovara.finovarabackend.expense.exception.notfound.ExpenseNotFoundException;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.expense.service.ExpenseService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.revenue.scoring.service.RevenueScoringService;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.AutoPaymentsMode;
import com.finovara.finovarabackend.usersetting.piggybank.roundup.service.RoundUpService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import com.finovara.finovarabackend.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteExpenseTest {
    @Mock
    private UserManagerService userManagerService;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private WalletService walletService;
    @Mock
    private RevenueScoringService revenueScoringService;
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

        verify(roundUpService).handleExpenseForRoundUp(email, expense.getId(), AutoPaymentsMode.ROLLBACK);
        verify(walletService).addBalanceToWallet(email, new BigDecimal("100"));
        verify(expenseActivityService).createExpenseActivity(email, ExpenseActivityType.DELETED_EXPENSE, expense);
        expenseRepository.delete(expense);
        verify(revenueScoringService).recalculateScore(email);
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
    }

}