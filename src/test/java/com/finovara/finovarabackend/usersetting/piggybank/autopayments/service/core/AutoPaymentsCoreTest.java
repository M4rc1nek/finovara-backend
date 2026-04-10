package com.finovara.finovarabackend.usersetting.piggybank.autopayments.service.core;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.service.AutoPaymentsCore;
import com.finovara.finovarabackend.wallet.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AutoPaymentsCoreTest {

    @Mock
    private PiggyBankActivityService piggyBankActivityService;

    @InjectMocks
    private AutoPaymentsCore autoPaymentsCore;

    private final String EMAIL = "test@test.com";

    private PiggyBank piggyBank;
    private Wallet wallet;

    @BeforeEach
    void setup() {
        piggyBank = new PiggyBank();
        piggyBank.setAmount(BigDecimal.valueOf(100));

        wallet = new Wallet();
        wallet.setBalance(BigDecimal.valueOf(500));
    }

    @Test
    void shouldApplyFullAmount() {
        autoPaymentsCore.apply(EMAIL, piggyBank, wallet, BigDecimal.valueOf(200));

        assertThat(piggyBank.getAmount()).isEqualByComparingTo("300");
        assertThat(wallet.getBalance()).isEqualByComparingTo("300");

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(
                eq(EMAIL),
                eq(piggyBank),
                eq(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING),
                eq(BigDecimal.valueOf(200))
        );
    }

    @Test
    void shouldApplyOnlyAvailableBalance() {
        wallet.setBalance(BigDecimal.valueOf(50));

        autoPaymentsCore.apply(EMAIL, piggyBank, wallet, BigDecimal.valueOf(200));

        assertThat(piggyBank.getAmount()).isEqualByComparingTo("150");
        assertThat(wallet.getBalance()).isEqualByComparingTo("0");

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(
                eq(EMAIL),
                eq(piggyBank),
                eq(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING),
                eq(BigDecimal.valueOf(50))
        );
    }

    @Test
    void shouldApplyZeroWhenWalletEmpty() {
        wallet.setBalance(BigDecimal.ZERO);

        autoPaymentsCore.apply(EMAIL, piggyBank, wallet, BigDecimal.valueOf(200));

        assertThat(piggyBank.getAmount()).isEqualByComparingTo("100");
        assertThat(wallet.getBalance()).isEqualByComparingTo("0");

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(
                eq(EMAIL),
                eq(piggyBank),
                eq(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING),
                eq(BigDecimal.ZERO)
        );
    }

    @Test
    void shouldRollbackFullAmount() {
        autoPaymentsCore.rollback(EMAIL, piggyBank, wallet, BigDecimal.valueOf(50));

        assertThat(piggyBank.getAmount()).isEqualByComparingTo("50");
        assertThat(wallet.getBalance()).isEqualByComparingTo("550");

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(
                eq(EMAIL),
                eq(piggyBank),
                eq(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING),
                eq(BigDecimal.valueOf(50))
        );
    }

    @Test
    void shouldRollbackOnlyAvailableInPiggyBank() {
        autoPaymentsCore.rollback(EMAIL, piggyBank, wallet, BigDecimal.valueOf(200));

        assertThat(piggyBank.getAmount()).isEqualByComparingTo("0");
        assertThat(wallet.getBalance()).isEqualByComparingTo("600");

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(
                eq(EMAIL),
                eq(piggyBank),
                eq(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING),
                eq(BigDecimal.valueOf(100))
        );
    }

    @Test
    void shouldRollbackZeroWhenPiggyBankEmpty() {
        piggyBank.setAmount(BigDecimal.ZERO);

        autoPaymentsCore.rollback(EMAIL, piggyBank, wallet, BigDecimal.valueOf(200));

        assertThat(piggyBank.getAmount()).isEqualByComparingTo("0");
        assertThat(wallet.getBalance()).isEqualByComparingTo("500");

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(
                eq(EMAIL),
                eq(piggyBank),
                eq(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING),
                eq(BigDecimal.ZERO)
        );
    }
}