package com.finovara.financeservice.settings.finances.recurring.service.validator;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.service.validator.util.RecurringBasicValidator;
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
