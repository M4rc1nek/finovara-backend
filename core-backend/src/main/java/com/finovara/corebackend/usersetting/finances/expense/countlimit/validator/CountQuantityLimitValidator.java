package com.finovara.corebackend.usersetting.finances.expense.countlimit.validator;

import com.finovara.contracts.exception.conflict.StateConflictException;
import com.finovara.contracts.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.corebackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import org.springframework.stereotype.Component;

@Component
public class CountQuantityLimitValidator {

    public void validateEmergencyMode(long countedExpenses, ConfirmPasswordDto confirmPasswordDto, ExpenseSettings expenseSettings) {
        if (expenseSettings.isQuantityLimitEmergencyModeUsed()) {
            throw new StateConflictException("Emergency mode already used. You have already added an expense using emergency mode in this period. You cannot add more expenses until the limit is increased or the period resets.");
        }

        if (!expenseSettings.isQuantityLimitEmergencyModeEnabled()) {
            throw new StateConflictException("Quantity Limit Exceeded, you have already added " + countedExpenses + " expenses");
        }

        if (confirmPasswordDto == null) {
            throw new MissingRequirementException("Emergency mode password confirmation required to continue");
        }
    }
}
