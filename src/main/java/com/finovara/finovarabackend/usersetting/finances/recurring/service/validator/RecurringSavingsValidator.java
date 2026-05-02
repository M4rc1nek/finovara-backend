package com.finovara.finovarabackend.usersetting.finances.recurring.service.validator;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.service.validator.util.RecurringBasicValidator;
import com.finovara.finovarabackend.wallet.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecurringSavingsValidator {
    private final RecurringBasicValidator recurringBasicValidator;

    public void validate(RecurringSettings settings, Wallet wallet) {
        recurringBasicValidator.validateBasicsWithoutCategory(settings);
        if (settings.getAmount().compareTo(wallet.getBalance()) > 0) {
            throw new InvalidInputException("Insufficient funds");
        }

        if (settings.getPiggyBankId() == null) {
            throw new InvalidInputException("Piggy bank is required");
        }
    }
}
