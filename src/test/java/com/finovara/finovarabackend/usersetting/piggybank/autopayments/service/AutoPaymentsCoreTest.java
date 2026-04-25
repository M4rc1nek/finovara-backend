package com.finovara.finovarabackend.usersetting.piggybank.autopayments.service;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.finovarabackend.wallet.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AutoPaymentsCoreTest {

    @Mock
    private PiggyBankActivityService piggyBankActivityService;

    @InjectMocks
    private AutoPaymentsCore autoPaymentsCore;

    private static final Long USER_ID = 1L;

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
        autoPaymentsCore.process(USER_ID, piggyBank, wallet, BigDecimal.valueOf(200), PiggyBankAutomationMode.APPLY);

        assertThat(piggyBank.getAmount()).isEqualByComparingTo("300");
        assertThat(wallet.getBalance()).isEqualByComparingTo("300");

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(USER_ID, piggyBank, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, BigDecimal.valueOf(200));
    }

    @Test
    void shouldApplyOnlyAvailableBalance() {
        wallet.setBalance(BigDecimal.valueOf(50));

        autoPaymentsCore.process(USER_ID, piggyBank, wallet, BigDecimal.valueOf(200), PiggyBankAutomationMode.APPLY);

        assertThat(piggyBank.getAmount()).isEqualByComparingTo("150");
        assertThat(wallet.getBalance()).isEqualByComparingTo("0");

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(USER_ID, piggyBank, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, BigDecimal.valueOf(50));
    }

    @Test
    void shouldApplyZeroWhenWalletEmpty() {
        wallet.setBalance(BigDecimal.ZERO);

        autoPaymentsCore.process(USER_ID, piggyBank, wallet, BigDecimal.valueOf(200), PiggyBankAutomationMode.APPLY);

        assertThat(piggyBank.getAmount()).isEqualByComparingTo("100");
        assertThat(wallet.getBalance()).isEqualByComparingTo("0");

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(USER_ID, piggyBank, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, BigDecimal.ZERO);
    }


    @Test
    void shouldRollbackFullAmount() {
        autoPaymentsCore.process(USER_ID, piggyBank, wallet, BigDecimal.valueOf(50), PiggyBankAutomationMode.ROLLBACK);

        assertThat(piggyBank.getAmount()).isEqualByComparingTo("50");
        assertThat(wallet.getBalance()).isEqualByComparingTo("550");

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(USER_ID, piggyBank, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, BigDecimal.valueOf(50));
    }

    @Test
    void shouldRollbackOnlyAvailableInPiggyBank() {
        autoPaymentsCore.process(USER_ID, piggyBank, wallet, BigDecimal.valueOf(200), PiggyBankAutomationMode.ROLLBACK);

        assertThat(piggyBank.getAmount()).isEqualByComparingTo("0");
        assertThat(wallet.getBalance()).isEqualByComparingTo("600");

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(USER_ID, piggyBank, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, BigDecimal.valueOf(100));
    }

    @Test
    void shouldRollbackZeroWhenPiggyBankEmpty() {
        piggyBank.setAmount(BigDecimal.ZERO);

        autoPaymentsCore.process(USER_ID, piggyBank, wallet, BigDecimal.valueOf(200), PiggyBankAutomationMode.ROLLBACK);

        assertThat(piggyBank.getAmount()).isEqualByComparingTo("0");
        assertThat(wallet.getBalance()).isEqualByComparingTo("500");

        verify(piggyBankActivityService).createPaymentPiggyBankActivity(USER_ID, piggyBank, PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, BigDecimal.ZERO);
    }
}