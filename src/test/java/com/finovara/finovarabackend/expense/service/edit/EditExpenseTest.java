package com.finovara.finovarabackend.expense.service.edit;

import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivityType;
import com.finovara.finovarabackend.accountactivity.expense.service.ExpenseActivityService;
import com.finovara.finovarabackend.expense.dto.ExpenseDTO;
import com.finovara.finovarabackend.expense.dto.ExpenseRequestDto;
import com.finovara.finovarabackend.expense.exception.notfound.ExpenseNotFoundException;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.expense.service.ExpenseService;
import com.finovara.finovarabackend.limit.model.LimitType;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.controlamount.service.ControlAmountService;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.model.CountQuantityLimitStrategy;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.service.SmartScanService;
import com.finovara.finovarabackend.usersetting.finances.revenue.scoring.service.RevenueScoringService;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.AutoPaymentsMode;
import com.finovara.finovarabackend.usersetting.piggybank.roundup.service.RoundUpService;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.service.expense.ExpenseManagerService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import com.finovara.finovarabackend.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EditExpenseTest {

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private ExpenseManagerService expenseManagerService;
    @Mock
    private WalletService walletService;
    @Mock
    private ExpenseActivityService expenseActivityService;
    @Mock
    private RoundUpService roundUpService;
    @Mock
    private ControlAmountService controlAmountService;
    @Mock
    private SmartScanService smartScanService;
    @Mock
    private UserManagerService userManagerService;
    @Mock
    private RevenueScoringService revenueScoringService;

    @InjectMocks
    private ExpenseService expenseService;

    @Test
    void shouldEditExpenseSuccessfully() {

        String email = "test@email.com";
        Long expenseId = 1L;

        User user = new User();
        user.setId(1L);

        Expense existingExpense = new Expense();
        existingExpense.setId(expenseId);
        existingExpense.setAmount(new BigDecimal("100"));
        BigDecimal oldAmount = existingExpense.getAmount();
        existingExpense.setCategory(ExpenseCategory.SAVINGS);
        ExpenseCategory oldCategory = existingExpense.getCategory();
        existingExpense.setDescription("old");

        User expenseUser = new User();
        expenseUser.setId(1L);
        existingExpense.setUserAssigned(expenseUser);

        ExpenseRequestDto dto = new ExpenseRequestDto(
                new ExpenseDTO(null, null, new BigDecimal("200"), ExpenseCategory.FOOD, null, "new description"),
                new ConfirmPasswordDto("pass"),
                new CountQuantityLimitDto(true, CountQuantityLimitStrategy.DAILY, 10)
        );

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(expenseManagerService.getExpenseByIdOrThrow(expenseId)).thenReturn(existingExpense);

        expenseService.editExpense(dto, email, expenseId, null);

        verify(walletService).addBalanceToWallet(email, new BigDecimal("100"));
        verify(walletService).removeBalanceFromWallet(email, new BigDecimal("200"));
        verify(roundUpService).handleExpenseForRoundUp(email, expenseId, AutoPaymentsMode.ROLLBACK);
        verify(expenseActivityService).updateExpenseActivity(email, ExpenseActivityType.EDITED_EXPENSE, existingExpense, oldAmount, oldCategory);
        verify(smartScanService).handleSmartScan(email, dto.confirmPasswordDto(), dto.expenseDTO().amount(), SmartScanMode.EDIT);
        verify(expenseRepository).save(existingExpense);
        verify(revenueScoringService).recalculateScore(email);
        verify(roundUpService).handleExpenseForRoundUp(email, expenseId, AutoPaymentsMode.APPLY);
        verify(controlAmountService).handleExpenseAmountControl(email, dto.expenseDTO().amount());
    }

    @Test
    void shouldThrowExceptionWhenExpenseDoesNotBelongToUser() {
        String email = "test@email.com";
        Long expenseId = 1L;

        User loggedUser = new User();
        loggedUser.setId(2L);

        User owner = new User();
        owner.setId(1L);

        Expense existingExpense = new Expense();
        existingExpense.setId(expenseId);
        existingExpense.setUserAssigned(owner);

        ExpenseRequestDto dto = new ExpenseRequestDto(
                new ExpenseDTO(null, null, new BigDecimal("200"), ExpenseCategory.FOOD, null, "test"),
                new ConfirmPasswordDto("pass"),
                new CountQuantityLimitDto(true, CountQuantityLimitStrategy.DAILY, 10)
        );

        when(expenseManagerService.getExpenseByIdOrThrow(expenseId)).thenReturn(existingExpense);
        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(loggedUser);

        assertThrows(ExpenseNotFoundException.class, () -> expenseService.editExpense(dto, email, expenseId, LimitType.DAILY));

        verify(expenseRepository, never()).save(any());

    }

    @Test
    void shouldThrowExceptionWhenExpenseDoesNotExist() {

        String email = "test@email.com";
        Long expenseId = 1L;

        ExpenseRequestDto dto = new ExpenseRequestDto(
                new ExpenseDTO(null, null, new BigDecimal("200"), ExpenseCategory.FOOD, null, "test"),
                new ConfirmPasswordDto("pass"),
                new CountQuantityLimitDto(true, CountQuantityLimitStrategy.DAILY, 10)
        );

        when(expenseManagerService.getExpenseByIdOrThrow(expenseId))
                .thenThrow(new ExpenseNotFoundException("Expense not found"));

        assertThrows(ExpenseNotFoundException.class, () -> expenseService.editExpense(dto, email, expenseId, LimitType.DAILY));
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        String email = "test@email.com";

        Long expenseId = 1L;

        ExpenseRequestDto dto = new ExpenseRequestDto(
                new ExpenseDTO(null, null, new BigDecimal("200"), ExpenseCategory.FOOD, null, "test"),
                new ConfirmPasswordDto("pass"),
                new CountQuantityLimitDto(true, CountQuantityLimitStrategy.DAILY, 10)
        );

        when(userManagerService.getUserByEmailOrThrow(email)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> expenseService.editExpense(dto, email, expenseId, null));
        verify(expenseRepository, never()).save(any());

    }
}
