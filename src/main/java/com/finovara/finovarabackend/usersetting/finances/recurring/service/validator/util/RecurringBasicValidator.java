package com.finovara.finovarabackend.usersetting.finances.recurring.service.validator.util;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecurringBasicValidator {
    public <C> void validateBasics(RecurringSettings settings, C category) {
        if (settings.getUserAssigned() == null) {
            throw new InvalidInputException("User is required");
        }

        if (settings.getStartDate() == null) {
            throw new InvalidInputException("Start date is required");
        }

        if (category == null) {
            throw new InvalidInputException("Category is required");
        }

        if (settings.getPeriodType() == null) {
            throw new InvalidInputException("Frequency is required");
        }

        if (settings.getAmount() == null) {
            throw new InvalidInputException("Amount is required");
        }
    }

}
