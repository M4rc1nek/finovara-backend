package com.finovara.finovarabackend.usersetting.piggybank.rondup.service.handle;

import com.finovara.finovarabackend.exception.notfound.WalletNotFoundException;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.finovarabackend.usersetting.piggybank.roundup.service.RoundUpCore;
import com.finovara.finovarabackend.usersetting.piggybank.roundup.service.RoundUpService;
import com.finovara.finovarabackend.util.expense.ExpenseManagerService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import com.finovara.finovarabackend.wallet.model.Wallet;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandleExpenseForRoundUpTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private ExpenseManagerService expenseManagerService;

    @Mock
    private PiggyBankRepository piggyBankRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private GoalCompletionService goalCompletionService;

    @Mock
    private RoundUpCore roundUpCore;

    @InjectMocks
    private RoundUpService roundUpService;

    private final String email = "test@test.com";
    private final Long expenseId = 1L;

    private User user;
    private Wallet wallet;
    private Expense expense;
    private PiggyBank piggyBank;

    @BeforeEach
    void setup() {

        user = new User();
        user.setEmail(email);
        user.setId(1L);

        wallet = new Wallet();
        wallet.setBalance(BigDecimal.valueOf(500));

        expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(123.45));

        piggyBank = new PiggyBank();
        piggyBank.setAmount(BigDecimal.valueOf(200));

        var settings = new com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings();
        settings.setRoundUpActive(true);
        piggyBank.setSettings(settings);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(expenseManagerService.getExpenseByUserIdOrThrow(expenseId, user.getId())).thenReturn(expense);
        when(piggyBankRepository.findAllByUserAssignedEmail(email)).thenReturn(List.of(piggyBank));
        when(walletRepository.findByUserAssignedEmail(email)).thenReturn(Optional.of(wallet));
    }

    @Test
    void shouldDoNothingWhenNoPiggyBanks() {

        when(piggyBankRepository.findAllByUserAssignedEmail(email)).thenReturn(List.of());

        roundUpService.handleExpenseForRoundUp(email, expenseId, PiggyBankAutomationMode.APPLY);

        verifyNoInteractions(roundUpCore);
        verifyNoInteractions(goalCompletionService);
    }

    @Test
    void shouldCallCoreApplyMode() {

        roundUpService.handleExpenseForRoundUp(email, expenseId, PiggyBankAutomationMode.APPLY);

        verify(roundUpCore).process(eq(email), eq(piggyBank), eq(wallet), any(BigDecimal.class), eq(PiggyBankAutomationMode.APPLY));

        verify(goalCompletionService).handleGoalCompletion(email);
    }

    @Test
    void shouldCallCoreRollbackMode() {

        roundUpService.handleExpenseForRoundUp(email, expenseId, PiggyBankAutomationMode.ROLLBACK);

        verify(roundUpCore).process(eq(email), eq(piggyBank), eq(wallet), any(BigDecimal.class), eq(PiggyBankAutomationMode.ROLLBACK));

        verify(goalCompletionService).handleGoalCompletion(email);
    }

    @Test
    void shouldThrowWhenWalletNotFound() {

        when(walletRepository.findByUserAssignedEmail(email)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> roundUpService.handleExpenseForRoundUp(email, expenseId, PiggyBankAutomationMode.APPLY));

        verifyNoInteractions(roundUpCore);
    }

    @Test
    void shouldSkipInactivePiggyBanks() {

        piggyBank.getSettings().setRoundUpActive(false);

        roundUpService.handleExpenseForRoundUp(email, expenseId, PiggyBankAutomationMode.APPLY);

        verifyNoInteractions(roundUpCore);
        verify(goalCompletionService).handleGoalCompletion(email);
    }
}