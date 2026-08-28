package com.finovara.financeservice.util.piggybank;

import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PiggyBankCalculatorTest {

    @Test
    void shouldCalculateProgressForPiggyBank() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(BigDecimal.valueOf(250));
        piggyBank.setGoalAmount(BigDecimal.valueOf(1000));

        Double progress = PiggyBankCalculator.calculateProgress(piggyBank);

        assertThat(progress).isEqualTo(25);
    }

    @Test
    void shouldCalculateProgressForSharedPiggyBank() {
        SharedPiggyBank sharedPiggyBank = new SharedPiggyBank();
        sharedPiggyBank.setAmount(BigDecimal.valueOf(500));
        sharedPiggyBank.setGoalAmount(BigDecimal.valueOf(1000));

        Double progress = PiggyBankCalculator.calculateSharedPiggyBankProgress(sharedPiggyBank);

        assertThat(progress).isEqualTo(50);
    }

    @Test
    void shouldReturnZeroWhenGoalAmountIsZeroForPiggyBank() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(BigDecimal.valueOf(100));
        piggyBank.setGoalAmount(BigDecimal.ZERO);

        Double progress = PiggyBankCalculator.calculateProgress(piggyBank);

        assertThat(progress).isZero();
    }

    @Test
    void shouldReturnZeroWhenGoalAmountIsNullForPiggyBank() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(BigDecimal.valueOf(100));
        piggyBank.setGoalAmount(null);

        Double progress = PiggyBankCalculator.calculateProgress(piggyBank);

        assertThat(progress).isZero();
    }

    @Test
    void shouldReturnZeroWhenGoalAmountIsZeroForSharedPiggyBank() {
        SharedPiggyBank sharedPiggyBank = new SharedPiggyBank();
        sharedPiggyBank.setAmount(BigDecimal.valueOf(100));
        sharedPiggyBank.setGoalAmount(BigDecimal.ZERO);

        Double progress = PiggyBankCalculator.calculateSharedPiggyBankProgress(sharedPiggyBank);

        assertThat(progress).isZero();
    }

    @Test
    void shouldReturnZeroWhenGoalAmountIsNullForSharedPiggyBank() {
        SharedPiggyBank sharedPiggyBank = new SharedPiggyBank();
        sharedPiggyBank.setAmount(BigDecimal.valueOf(100));
        sharedPiggyBank.setGoalAmount(null);

        Double progress = PiggyBankCalculator.calculateSharedPiggyBankProgress(sharedPiggyBank);

        assertThat(progress).isZero();
    }

    @Test
    void shouldRoundProgressToFourDecimalPlaces() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(BigDecimal.ONE);
        piggyBank.setGoalAmount(BigDecimal.valueOf(3));

        Double progress = PiggyBankCalculator.calculateProgress(piggyBank);

        assertThat(progress).isEqualTo(33.33);
    }
}