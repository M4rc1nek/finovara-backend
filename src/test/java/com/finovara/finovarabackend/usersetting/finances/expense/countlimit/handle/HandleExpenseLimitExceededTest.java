package com.finovara.finovarabackend.usersetting.finances.expense.countlimit.handle;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.service.CountQuantityLimitService;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.validator.CountQuantityLimitValidator;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordConfirmationService;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandleExpenseLimitExceededTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private PasswordConfirmationService passwordConfirmationService;

    @Mock
    private CountQuantityLimitValidator countQuantityLimitValidator;

    @InjectMocks
    private CountQuantityLimitService countQuantityLimitService;

    private ExpenseSettings expenseSettings;
    private final String EMAIL = "test@test.com";

    @BeforeEach
    void setup() {
        User user = new User();
        user.setId(1L);
        expenseSettings = new ExpenseSettings();
        user.setExpenseSettings(expenseSettings);

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
    }

    @Test
    void shouldDoNothingWhenLimitDisabled() {
        expenseSettings.setCountQuantityLimitEnabled(false);

        countQuantityLimitService.handleExpenseLimitExceeded(
                EMAIL,
                new CountQuantityLimitDto(true, PeriodType.DAILY, 5),
                PeriodType.DAILY,
                null
        );

        verifyNoInteractions(passwordConfirmationService);
    }

    @Test
    void shouldUseEmergencyModeWhenPasswordProvided() {
        expenseSettings.setCountQuantityLimitEnabled(true);
        expenseSettings.setQuantityLimitEmergencyModeEnabled(true);

        when(expenseRepository.countExpensesByUserAssignedIdAndCreatedAtBetween(eq(1L), any(), any()))
                .thenReturn(5L);

        CountQuantityLimitDto dto = new CountQuantityLimitDto(true, PeriodType.DAILY, 5);
        ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto("password");

        countQuantityLimitService.handleExpenseLimitExceeded(
                EMAIL,
                dto,
                PeriodType.DAILY,
                confirmPasswordDto
        );

        verify(passwordConfirmationService).confirmPassword(EMAIL, confirmPasswordDto);
        assertFalse(expenseSettings.isQuantityLimitEmergencyModeEnabled());
        assertTrue(expenseSettings.isQuantityLimitEmergencyModeUsed());
    }
}