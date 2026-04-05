package com.finovara.finovarabackend.util.service.piggybank;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.piggybank.dto.PiggyBankDTO;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;

@UtilityClass
public class PiggyBankValidator {

    public static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInputException("Amount must be non negative");
        }
    }

    public static void validateGoalAmount(PiggyBankDTO dto) {
        if (dto.goalAmount() != null && dto.goalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInputException("Amount have to be positive");
        }
    }
}