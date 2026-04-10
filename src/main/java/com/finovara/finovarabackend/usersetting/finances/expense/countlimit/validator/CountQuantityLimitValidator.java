package com.finovara.finovarabackend.usersetting.finances.expense.countlimit.validator;

import com.finovara.finovarabackend.exception.conflict.StateConflictException;
import com.finovara.finovarabackend.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CountQuantityLimitValidator {
    private final ExpenseSettings expenseSettings;

    public void validateEmergencyMode(long countedExpenses, ConfirmPasswordDto confirmPasswordDto) {
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
