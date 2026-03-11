package com.finovara.finovarabackend.expense.service.add;

import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivityType;
import com.finovara.finovarabackend.accountactivity.expense.service.ExpenseActivityService;
import com.finovara.finovarabackend.config.TimeConfig;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.expense.dto.ExpenseDTO;
import com.finovara.finovarabackend.expense.dto.ExpenseRequestDto;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.expense.service.ExpenseService;
import com.finovara.finovarabackend.limit.exception.unprocessablecontent.LimitExceededException;
import com.finovara.finovarabackend.limit.model.LimitType;
import com.finovara.finovarabackend.limit.repository.LimitRepository;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.controlamount.service.ControlAmountService;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.model.CountQuantityLimitStrategy;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.service.CountQuantityLimitService;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.service.SmartScanService;
import com.finovara.finovarabackend.usersetting.finances.revenue.scoring.service.RevenueScoringService;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.AutoPaymentsMode;
import com.finovara.finovarabackend.usersetting.piggybank.roundup.service.RoundUpService;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.service.time.SpentInPeriodService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import com.finovara.finovarabackend.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddExpenseTest {

    @InjectMocks
    private ExpenseService expenseService;

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private LimitRepository limitRepository;
    @Mock
    private WalletService walletService;
    @Mock
    private ExpenseActivityService expenseActivityService;
    @Mock
    private RoundUpService roundUpService;
    @Mock
    private CountQuantityLimitService countQuantityLimitService;
    @Mock
    private ControlAmountService controlAmountService;
    @Mock
    private SmartScanService smartScanService;
    @Mock
    private UserManagerService userManagerService;
    @Mock
    private SpentInPeriodService spentInPeriodService;
    @Mock
    private RevenueScoringService revenueScoringService;
    @Mock
    private TimeConfig timeConfig;

    @Test
    void shouldAddExpenseSuccessfully() {

        // given
        String email = "test@email.com";
        BigDecimal amount = new BigDecimal("100");

        User user = new User();
        user.setId(1L);

        ExpenseRequestDto dto = new ExpenseRequestDto(
                new ExpenseDTO(null, null, amount, ExpenseCategory.SAVINGS, null, "test"),
                new ConfirmPasswordDto("password"),
                new CountQuantityLimitDto(true, CountQuantityLimitStrategy.DAILY, 10)
        );

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(limitRepository.getLimitAmountByUserIdAndType(anyLong(), any())).thenReturn(Optional.empty());
        when(spentInPeriodService.getSpentToday(anyLong())).thenReturn(BigDecimal.ZERO);
        when(timeConfig.clock()).thenReturn(Clock.systemUTC());
        when(expenseRepository.save(any())).thenAnswer(invocation -> {
            Expense expense = invocation.getArgument(0);
            expense.setId(1L);
            return expense;
        });

        // when
        Long result = expenseService.addExpense(dto, email, LimitType.DAILY);

        // then
        assertEquals(1L, result);

        verify(countQuantityLimitService).calculateCountQuantityLimit(email, dto.countQuantityLimitDto(),
                dto.countQuantityLimitDto().countQuantityLimitStrategy(), dto.confirmPasswordDto());

        verify(expenseActivityService).createExpenseActivity(eq(email), eq(ExpenseActivityType.ADDED_EXPENSE), any());
        verify(smartScanService).handleSmartScan(email, dto.confirmPasswordDto(), amount, SmartScanMode.ADD);

        verify(walletService).removeBalanceFromWallet(email, amount);

        verify(expenseRepository).save(any(Expense.class));

        verify(revenueScoringService).recalculateScore(email);
        verify(roundUpService).handleExpenseForRoundUp(eq(email), anyLong(), eq(AutoPaymentsMode.APPLY));

        verify(controlAmountService).handleExpenseAmountControl(email, amount);
    }

    @Test
    void shouldThrowExceptionWhenAmountIsLessThanOne() {
        String email = "sas@op.pl";
        User user = new User();
        user.setId(1L);

        ExpenseRequestDto dto = new ExpenseRequestDto(
                new ExpenseDTO(null, null, new BigDecimal("0.50"), ExpenseCategory.SAVINGS, null, "test"),
                new ConfirmPasswordDto("pass"),
                new CountQuantityLimitDto(true, CountQuantityLimitStrategy.DAILY, 10)
        );

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(timeConfig.clock()).thenReturn(Clock.systemUTC());
        when(spentInPeriodService.getSpentToday(anyLong())).thenReturn(BigDecimal.ZERO);

        assertThrows(InvalidInputException.class, () -> expenseService.addExpense(dto, email, LimitType.DAILY));
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        String email = "sas@op.pl";

        ExpenseRequestDto dto = new ExpenseRequestDto(
                new ExpenseDTO(null, null, new BigDecimal("0.50"), ExpenseCategory.SAVINGS, null, "test"),
                new ConfirmPasswordDto("pass"),
                new CountQuantityLimitDto(true, CountQuantityLimitStrategy.DAILY, 10)
        );

        when(userManagerService.getUserByEmailOrThrow(email)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> expenseService.addExpense(dto, email, null));
        verify(expenseRepository, never()).save(any());
    }
}
