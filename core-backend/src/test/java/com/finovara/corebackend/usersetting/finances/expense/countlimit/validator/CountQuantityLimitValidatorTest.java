package com.finovara.corebackend.usersetting.finances.expense.countlimit.validator;

import com.finovara.contracts.exception.conflict.StateConflictException;
import com.finovara.contracts.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.corebackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CountQuantityLimitValidatorTest {
    private CountQuantityLimitValidator countQuantityLimitValidator;
    private ExpenseSettings expenseSettings;

    @BeforeEach
    void setup() {
        expenseSettings = new ExpenseSettings();
        countQuantityLimitValidator = new CountQuantityLimitValidator();
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
                () -> countQuantityLimitValidator.validateEmergencyMode(5L, new ConfirmPasswordDto("password"), expenseSettings));
    }

    @Test
    void shouldThrowMissingRequirementIfEmergencyModeEnabledButNoPassword() {
        expenseSettings.setQuantityLimitEmergencyModeUsed(false);
        expenseSettings.setQuantityLimitEmergencyModeEnabled(true);

        assertThrows(MissingRequirementException.class,
                () -> countQuantityLimitValidator.validateEmergencyMode(5L, null, expenseSettings));
    }
}