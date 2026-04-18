package com.finovara.finovarabackend.usersetting.finances.expense.controlamount.service.handle;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.controlamount.service.ControlAmountService;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandleExpenseAmountControlTest {

    @Mock
    private UserManagerService userManagerService;

    @InjectMocks
    private ControlAmountService controlAmountService;

    private User user;
    private ExpenseSettings expenseSettings;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setup() {
        user = new User();
        expenseSettings = new ExpenseSettings();
        user.setExpenseSettings(expenseSettings);
    }

    @Test
    void shouldThrowExceptionWhenAmountExceedsBlockedAmount() {
        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

        expenseSettings.setAmountThresholdEnabled(true);
        expenseSettings.setBlockedAmount(BigDecimal.valueOf(100));

        BigDecimal newAmount = BigDecimal.valueOf(150);

        assertThrows(InvalidInputException.class, () -> controlAmountService.handleExpenseAmountControl(USER_ID, newAmount));
    }

    @Test
    void shouldAllowAmountEqualOrLessThanBlockedAmount() {
        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

        expenseSettings.setAmountThresholdEnabled(true);
        expenseSettings.setBlockedAmount(BigDecimal.valueOf(100));

        BigDecimal newAmount = BigDecimal.valueOf(100);

        controlAmountService.handleExpenseAmountControl(USER_ID, newAmount);
    }

    @Test
    void shouldAllowAnyAmountWhenControlIsDisabled() {
        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

        expenseSettings.setAmountThresholdEnabled(false);
        expenseSettings.setBlockedAmount(BigDecimal.valueOf(50));

        BigDecimal newAmount = BigDecimal.valueOf(1000);

        controlAmountService.handleExpenseAmountControl(USER_ID, newAmount);
    }

    @Test
    void shouldTreatNullBlockedAmountAsZero() {
        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

        expenseSettings.setAmountThresholdEnabled(true);
        expenseSettings.setBlockedAmount(null);

        BigDecimal newAmount = BigDecimal.valueOf(10);

        assertThrows(InvalidInputException.class, () -> controlAmountService.handleExpenseAmountControl(USER_ID, newAmount));
    }
}