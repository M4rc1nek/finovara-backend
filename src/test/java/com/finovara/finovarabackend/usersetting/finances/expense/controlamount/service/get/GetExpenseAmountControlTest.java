package com.finovara.finovarabackend.usersetting.finances.expense.controlamount.service.get;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.controlamount.dto.ControlAmountDto;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlAmountServiceGetTest {

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
    void shouldReturnCorrectControlAmountDtoWhenEnabled() {
        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

        expenseSettings.setAmountThresholdEnabled(true);
        expenseSettings.setBlockedAmount(BigDecimal.valueOf(200));

        ControlAmountDto result = controlAmountService.getExpenseAmountControl(USER_ID);

        assertTrue(result.expenseAmountThresholdEnabled());
        assertEquals(BigDecimal.valueOf(200), result.blockedAmount());
    }

    @Test
    void shouldReturnCorrectControlAmountDtoWhenDisabled() {
        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

        expenseSettings.setAmountThresholdEnabled(false);
        expenseSettings.setBlockedAmount(BigDecimal.valueOf(50));

        ControlAmountDto result = controlAmountService.getExpenseAmountControl(USER_ID);

        assertFalse(result.expenseAmountThresholdEnabled());
        assertEquals(BigDecimal.valueOf(50), result.blockedAmount());
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenThrow(new InvalidInputException("User not found"));

        assertThrows(InvalidInputException.class, () -> controlAmountService.getExpenseAmountControl(USER_ID));
    }
}