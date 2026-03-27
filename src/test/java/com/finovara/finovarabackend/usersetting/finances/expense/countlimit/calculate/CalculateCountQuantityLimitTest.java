package com.finovara.finovarabackend.usersetting.finances.expense.countlimit.calculate;

import com.finovara.finovarabackend.exception.conflict.StateConflictException;
import com.finovara.finovarabackend.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.model.CountQuantityLimitStrategy;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.service.CountQuantityLimitService;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordConfirmationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalculateCountQuantityLimitSimpleTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private PasswordConfirmationService passwordConfirmationService;

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
        expenseSettings.setExpenseCountQuantityLimitEnabled(false);
        countQuantityLimitService.calculateCountQuantityLimit(EMAIL,
                new CountQuantityLimitDto(true, CountQuantityLimitStrategy.DAILY, 5),
                CountQuantityLimitStrategy.DAILY,
                null);

        verifyNoInteractions(passwordConfirmationService);
    }

    @Test
    void shouldThrowWhenLimitExceededAndEmergencyModeUsed() {
        expenseSettings.setExpenseCountQuantityLimitEnabled(true);
        expenseSettings.setExpenseQuantityLimitEmergencyModeUsed(true);

        when(expenseRepository.countExpensesByUserAssignedIdAndCreatedAtBetween(eq(1L), any(), any())).thenReturn(5L);

        CountQuantityLimitDto dto = new CountQuantityLimitDto(true, CountQuantityLimitStrategy.DAILY, 5);

        assertThrows(StateConflictException.class, () -> countQuantityLimitService.calculateCountQuantityLimit(EMAIL, dto, CountQuantityLimitStrategy.DAILY, null));
    }

    @Test
    void shouldThrowWhenLimitExceededAndEmergencyModeDisabled() {
        expenseSettings.setExpenseCountQuantityLimitEnabled(true);
        expenseSettings.setExpenseQuantityLimitEmergencyModeEnabled(false);

        when(expenseRepository.countExpensesByUserAssignedIdAndCreatedAtBetween(eq(1L), any(), any())).thenReturn(5L);

        CountQuantityLimitDto dto = new CountQuantityLimitDto(true, CountQuantityLimitStrategy.DAILY, 5);

        assertThrows(StateConflictException.class, () -> countQuantityLimitService.calculateCountQuantityLimit(EMAIL, dto, CountQuantityLimitStrategy.DAILY, null));
    }

    @Test
    void shouldThrowMissingRequirementIfEmergencyModeEnabledButNoPassword() {
        expenseSettings.setExpenseCountQuantityLimitEnabled(true);
        expenseSettings.setExpenseQuantityLimitEmergencyModeEnabled(true);

        when(expenseRepository.countExpensesByUserAssignedIdAndCreatedAtBetween(eq(1L), any(), any())).thenReturn(5L);

        CountQuantityLimitDto dto = new CountQuantityLimitDto(true, CountQuantityLimitStrategy.DAILY, 5);

        assertThrows(MissingRequirementException.class, () -> countQuantityLimitService.calculateCountQuantityLimit(EMAIL, dto, CountQuantityLimitStrategy.DAILY, null));
    }

    @Test
    void shouldUseEmergencyModeWhenPasswordProvided() {
        expenseSettings.setExpenseCountQuantityLimitEnabled(true);
        expenseSettings.setExpenseQuantityLimitEmergencyModeEnabled(true);

        when(expenseRepository.countExpensesByUserAssignedIdAndCreatedAtBetween(eq(1L), any(), any())).thenReturn(5L);

        CountQuantityLimitDto dto = new CountQuantityLimitDto(true, CountQuantityLimitStrategy.DAILY, 5);
        ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto("password");

        countQuantityLimitService.calculateCountQuantityLimit(EMAIL, dto, CountQuantityLimitStrategy.DAILY, confirmPasswordDto);

        verify(passwordConfirmationService).confirmPassword(EMAIL, confirmPasswordDto);
        assert !expenseSettings.isExpenseQuantityLimitEmergencyModeEnabled();
        assert expenseSettings.isExpenseQuantityLimitEmergencyModeUsed();
    }
}