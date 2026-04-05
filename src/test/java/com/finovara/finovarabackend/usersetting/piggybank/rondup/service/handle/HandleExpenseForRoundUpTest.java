package com.finovara.finovarabackend.usersetting.piggybank.rondup.service.handle;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.exception.notfound.WalletNotFoundException;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.AutoPaymentsMode;
import com.finovara.finovarabackend.usersetting.piggybank.completion.service.GoalCompletionService;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.usersetting.piggybank.roundup.service.RoundUpService;
import com.finovara.finovarabackend.util.expense.ExpenseManagerService;
import com.finovara.finovarabackend.util.piggybank.manager.PiggyBankManagerService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandleExpenseForRoundUpTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private ExpenseManagerService expenseManagerService;
    @Mock
    private PiggyBankManagerService piggyBankManagerService;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private PiggyBankActivityService piggyBankActivityService;
    @Mock
    private PiggyBankRepository piggyBankRepository;
    @Mock
    private GoalCompletionService goalCompletionService;

    @InjectMocks
    private RoundUpService roundUpService;

    private PiggyBank piggyBank;
    private Wallet wallet;
    private Expense expense;

    private final String EMAIL = "test@test.com";
    private final Long EXPENSE_ID = 1L;

    @BeforeEach
    void setup() {

        User user = new User();
        user.setEmail(EMAIL);
        user.setId(1L);

        wallet = new Wallet();
        wallet.setBalance(BigDecimal.valueOf(500));

        piggyBank = new PiggyBank();
        piggyBank.setAmount(BigDecimal.valueOf(200));

        PiggyBankSettings settings = new PiggyBankSettings();
        settings.setRoundUpActive(false);
        piggyBank.setSettings(settings);

        expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(123.45));

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
        when(walletRepository.findByUserAssignedEmail(EMAIL)).thenReturn(Optional.of(wallet));
        when(piggyBankRepository.findAllByUserAssignedEmail(EMAIL)).thenReturn(List.of(piggyBank));
        when(expenseManagerService.getExpenseByUserIdOrThrow(EXPENSE_ID, user.getId())).thenReturn(expense);
    }

    @Test
    void shouldDoNothingWhenNoRoundUpActive() {

        piggyBank.getSettings().setRoundUpActive(false);

        roundUpService.handleExpenseForRoundUp(EMAIL, EXPENSE_ID, AutoPaymentsMode.APPLY);

        assertEquals(BigDecimal.valueOf(200), piggyBank.getAmount());
        assertEquals(BigDecimal.valueOf(500), wallet.getBalance());

        verifyNoInteractions(piggyBankActivityService);
    }

    @Test
    void shouldApplyRoundUpSuccessfully() {

        piggyBank.getSettings().setRoundUpActive(true);

        roundUpService.handleExpenseForRoundUp(EMAIL, EXPENSE_ID, AutoPaymentsMode.APPLY);

        BigDecimal expectedRoundUp = BigDecimal.valueOf(0.55);

        assertEquals(BigDecimal.valueOf(200.55), piggyBank.getAmount());
        assertEquals(BigDecimal.valueOf(499.45), wallet.getBalance());

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(
                EMAIL,
                piggyBank,
                PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING,
                expectedRoundUp
        );

        verify(goalCompletionService).handleGoalCompletion(EMAIL);
    }

    @Test
    void shouldRollbackRoundUpSuccessfully() {

        piggyBank.getSettings().setRoundUpActive(true);

        piggyBank.setAmount(BigDecimal.valueOf(5));
        wallet.setBalance(BigDecimal.valueOf(10));

        roundUpService.handleExpenseForRoundUp(EMAIL, EXPENSE_ID, AutoPaymentsMode.ROLLBACK);

        BigDecimal expectedRoundUp = BigDecimal.valueOf(0.55);

        assertEquals(BigDecimal.valueOf(4.45), piggyBank.getAmount());
        assertEquals(BigDecimal.valueOf(10.55), wallet.getBalance());

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(
                EMAIL,
                piggyBank,
                PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING,
                expectedRoundUp
        );
    }

    @Test
    void shouldThrowWhenInsufficientFunds() {

        piggyBank.getSettings().setRoundUpActive(true);

        expense.setAmount(BigDecimal.valueOf(9.80));
        wallet.setBalance(BigDecimal.valueOf(0.10));

        assertThrows(InvalidInputException.class, () -> roundUpService.handleExpenseForRoundUp(EMAIL, EXPENSE_ID, AutoPaymentsMode.APPLY));
    }

    @Test
    void shouldThrowExceptionWhenWalletDoesNotExist() {
        when(walletRepository.findByUserAssignedEmail(EMAIL)).thenReturn(Optional.empty());
        assertThrows(WalletNotFoundException.class, () -> roundUpService.handleExpenseForRoundUp(EMAIL, EXPENSE_ID, AutoPaymentsMode.APPLY));

    }
}