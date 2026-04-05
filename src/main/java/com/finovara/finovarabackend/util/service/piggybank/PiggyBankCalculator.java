package com.finovara.finovarabackend.util.service.piggybank;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;

@UtilityClass
public class PiggyBankCalculator {
    public static Double calculateProgress(PiggyBank piggyBank) {
        BigDecimal goalAmount = piggyBank.getGoalAmount();

        if (goalAmount == null || goalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }

        return piggyBank.getAmount()
                .divide(piggyBank.getGoalAmount(), 4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static void validateSufficientFunds(BigDecimal sourceAmount, BigDecimal amount) {
        if (sourceAmount == null || sourceAmount.compareTo(amount) < 0) {
            throw new InvalidInputException("Insufficient funds");
        }
    }
}
