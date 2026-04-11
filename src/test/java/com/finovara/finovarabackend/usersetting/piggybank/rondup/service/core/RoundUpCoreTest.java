package com.finovara.finovarabackend.usersetting.piggybank.rondup.service.core;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.finovarabackend.usersetting.piggybank.roundup.service.RoundUpCore;
import com.finovara.finovarabackend.wallet.model.Wallet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoundUpCoreTest {

    @Mock
    private PiggyBankActivityService piggyBankActivityService;

    @InjectMocks
    private RoundUpCore roundUpCore;

    private final String EMAIL = "test@test.com";

    @Test
    void shouldApplyRoundUpSuccessfully() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("10.00"));

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("50.00"));

        BigDecimal roundUp = new BigDecimal("5.00");

        roundUpCore.process(EMAIL, piggyBank, wallet, roundUp, PiggyBankAutomationMode.APPLY);

        assertEquals(new BigDecimal("15.00"), piggyBank.getAmount());
        assertEquals(new BigDecimal("45.00"), wallet.getBalance());

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(eq(EMAIL), eq(piggyBank), eq(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING), eq(roundUp));
    }

    @Test
    void shouldRollbackSuccessfully() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("20.00"));

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("10.00"));

        BigDecimal roundUp = new BigDecimal("5.00");

        roundUpCore.process(EMAIL, piggyBank, wallet, roundUp, PiggyBankAutomationMode.ROLLBACK);

        assertEquals(new BigDecimal("15.00"), piggyBank.getAmount());
        assertEquals(new BigDecimal("15.00"), wallet.getBalance());

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(eq(EMAIL), eq(piggyBank), eq(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING), eq(roundUp));
    }

    @Test
    void shouldRollbackOnlyAvailableAmount() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("3.00"));

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("10.00"));

        BigDecimal roundUp = new BigDecimal("5.00");

        roundUpCore.process(EMAIL, piggyBank, wallet, roundUp, PiggyBankAutomationMode.ROLLBACK);

        assertThat(piggyBank.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(13));

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(eq(EMAIL), eq(piggyBank), eq(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING), eq(new BigDecimal("3.00")));
    }

    @Test
    void shouldDoNothingWhenRoundUpIsZeroOrNegative() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("10.00"));

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("50.00"));

        roundUpCore.process(EMAIL, piggyBank, wallet, BigDecimal.ZERO, PiggyBankAutomationMode.APPLY);

        assertEquals(new BigDecimal("10.00"), piggyBank.getAmount());
        assertEquals(new BigDecimal("50.00"), wallet.getBalance());

        verifyNoInteractions(piggyBankActivityService);
    }

    @Test
    void shouldDoNothingWhenRollbackAmountIsZero() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(BigDecimal.ZERO);

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("10.00"));

        roundUpCore.process(EMAIL, piggyBank, wallet, new BigDecimal("5.00"), PiggyBankAutomationMode.ROLLBACK);

        assertEquals(BigDecimal.ZERO, piggyBank.getAmount());
        assertEquals(new BigDecimal("10.00"), wallet.getBalance());

        verifyNoInteractions(piggyBankActivityService);
    }

    @Test
    void shouldThrowWhenInsufficientFunds() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("10.00"));

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("2.00"));

        BigDecimal roundUp = new BigDecimal("5.00");

        assertThrows(InvalidInputException.class, () -> roundUpCore.process(EMAIL, piggyBank, wallet, roundUp, PiggyBankAutomationMode.APPLY));

        verifyNoInteractions(piggyBankActivityService);
    }
}