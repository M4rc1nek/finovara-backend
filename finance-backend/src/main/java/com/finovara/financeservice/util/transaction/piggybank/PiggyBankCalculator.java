package com.finovara.financeservice.util.piggybank;

import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

@UtilityClass
@Slf4j
public class PiggyBankCalculator {

    public static Double calculateProgress(PiggyBank piggyBank) {
        return calculateProgressTemplate(
                piggyBank.getAmount(),
                piggyBank.getGoalAmount()
        );
    }

    public static Double calculateSharedPiggyBankProgress(SharedPiggyBank sharedPiggyBank) {
        return calculateProgressTemplate(
                sharedPiggyBank.getAmount(),
                sharedPiggyBank.getGoalAmount()
        );
    }

    private static Double calculateProgressTemplate(BigDecimal amount, BigDecimal goalAmount) {
        if (goalAmount == null || goalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }

        Double result = amount.divide(goalAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();

        log.info("Balance in PiggyBank: {} GoalAmount: {} Result: {}", amount, goalAmount, result);

        return result;
    }
}