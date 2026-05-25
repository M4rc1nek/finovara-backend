package com.finovara.corebackend.usersetting.finances.recurring.service.validator;

import com.finovara.corebackend.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.corebackend.usersetting.finances.recurring.service.validator.util.RecurringBasicValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class RecurringRevenueValidator {
    private final RecurringBasicValidator recurringBasicValidator;

    public void validate(RecurringSettings settings) {
        recurringBasicValidator.validateBasics(settings, settings.getRevenueCategory());
        if (settings.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInputException("Recurring revenue must be greater than 0");
        }

        if (settings.getAmount().compareTo(BigDecimal.valueOf(5000000)) > 0) {
            throw new InvalidInputException("Recurring revenue cannot be greater than 5000000");
        }

    }
}
