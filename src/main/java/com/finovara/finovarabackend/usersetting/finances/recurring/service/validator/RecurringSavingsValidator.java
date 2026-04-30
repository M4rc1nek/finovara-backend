package com.finovara.finovarabackend.usersetting.finances.recurring.service.validator;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.wallet.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecurringSavingsValidator {
    public void validate(RecurringSettings recurringSettings, Wallet wallet) {
        if (recurringSettings.getAmount().compareTo(wallet.getBalance()) > 0) {
            throw new InvalidInputException("Insufficient funds");
        }
    }
}
