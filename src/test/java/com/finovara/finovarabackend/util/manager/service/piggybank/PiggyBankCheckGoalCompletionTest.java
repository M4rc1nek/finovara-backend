package com.finovara.finovarabackend.util.manager.service.piggybank;

import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.util.service.piggybank.PiggyBankCheckGoalCompletion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class PiggyBankCheckGoalCompletionTest {

    @InjectMocks
    private PiggyBankCheckGoalCompletion checkGoalCompletion;

    @Test
    void shouldReturnFalseWhenGoalAmountIsNull() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("100.00"));
        piggyBank.setGoalAmount(null);

        boolean result = checkGoalCompletion.isGoalCompleted(piggyBank);

        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenAmountIsLessThanGoalAmount() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("50.00"));
        piggyBank.setGoalAmount(new BigDecimal("100.00"));

        boolean result = checkGoalCompletion.isGoalCompleted(piggyBank);

        assertFalse(result);
    }

    @Test
    void shouldReturnTrueWhenAmountEqualsGoalAmount() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("100.00"));
        piggyBank.setGoalAmount(new BigDecimal("100.00"));

        boolean result = checkGoalCompletion.isGoalCompleted(piggyBank);

        assertTrue(result);
    }

    @Test
    void shouldReturnTrueWhenAmountIsGreaterThanGoalAmount() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("150.00"));
        piggyBank.setGoalAmount(new BigDecimal("100.00"));

        boolean result = checkGoalCompletion.isGoalCompleted(piggyBank);

        assertTrue(result);
    }
}