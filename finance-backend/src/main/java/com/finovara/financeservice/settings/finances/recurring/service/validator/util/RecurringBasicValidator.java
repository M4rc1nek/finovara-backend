package com.finovara.financeservice.settings.finances.recurring.service.validator.util;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import org.springframework.stereotype.Service;

@Service
public class RecurringBasicValidator {

    public <C> void validateBasics(RecurringSettings settings, C category) {
        validateUser(settings);
        validateStartDate(settings);
        validateEndDate(settings);
        validateCategory(category);
        validatePeriodType(settings);
        validateAmount(settings);
    }

    public void validateBasicsWithoutCategory(RecurringSettings settings) {
        validateUser(settings);
        validateStartDate(settings);
        validateEndDate(settings);
        validatePeriodType(settings);
        validateAmount(settings);
    }

    private void validateUser(RecurringSettings settings) {
        if (settings.getUserId() == null) {
            throw new InvalidInputException("User is required");
        }
    }

    private void validateStartDate(RecurringSettings settings) {
        if (settings.getStartDate() == null) {
            throw new InvalidInputException("Start date is required");
        }
    }

    private void validateEndDate(RecurringSettings settings) {
        if (settings.getEndDate() == null) {
            throw new InvalidInputException("End date is required");
        }
    }

    private <C> void validateCategory(C category) {
        if (category == null) {
            throw new InvalidInputException("Category is required");
        }
    }

    private void validatePeriodType(RecurringSettings settings) {
        if (settings.getPeriodType() == null) {
            throw new InvalidInputException("Frequency is required");
        }
    }

    private void validateAmount(RecurringSettings settings) {
        if (settings.getAmount() == null) {
            throw new InvalidInputException("Amount is required");
        }
    }
}