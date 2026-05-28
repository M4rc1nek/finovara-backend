package com.finovara.corebackend.util.piggybank;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.piggybank.dto.PiggyBankDto;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;

@UtilityClass
public class PiggyBankValidator {

    public static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInputException("Amount must be non negative");
        }
    }

    public static void validateGoalAmount(PiggyBankDto dto) {
        if (dto.goalAmount() != null && dto.goalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInputException("Amount have to be positive");
        }
    }

    public static void validateSufficientFunds(BigDecimal sourceAmount, BigDecimal amount) {
        if (sourceAmount == null || sourceAmount.compareTo(amount) < 0) {
            throw new InvalidInputException("Insufficient funds");
        }
    }
}