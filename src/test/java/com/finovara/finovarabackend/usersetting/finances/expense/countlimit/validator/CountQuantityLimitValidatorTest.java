package com.finovara.finovarabackend.usersetting.finances.expense.countlimit.validator;

import com.finovara.finovarabackend.exception.conflict.StateConflictException;
import com.finovara.finovarabackend.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CountQuantityLimitValidatorTest {
    private ExpenseSettings expenseSettings;
    private CountQuantityLimitValidator countQuantityLimitValidator;

    @BeforeEach
    void setup() {
        expenseSettings = new ExpenseSettings();
        countQuantityLimitValidator = new CountQuantityLimitValidator(expenseSettings);
    }

    @ParameterizedTest
    @CsvSource({
            "true, true",
            "true, false"
    })
    void shouldThrowStateConflictException(boolean emergencyModeUsed, boolean emergencyModeEnabled) {
        expenseSettings.setQuantityLimitEmergencyModeUsed(emergencyModeUsed);
        expenseSettings.setQuantityLimitEmergencyModeEnabled(emergencyModeEnabled);

        assertThrows(StateConflictException.class,
                () -> countQuantityLimitValidator.validateEmergencyMode(5L, new ConfirmPasswordDto("password")));
    }

    @Test
    void shouldThrowMissingRequirementIfEmergencyModeEnabledButNoPassword() {
        expenseSettings.setQuantityLimitEmergencyModeUsed(false);
        expenseSettings.setQuantityLimitEmergencyModeEnabled(true);

        assertThrows(MissingRequirementException.class,
                () -> countQuantityLimitValidator.validateEmergencyMode(5L, null));
    }
}