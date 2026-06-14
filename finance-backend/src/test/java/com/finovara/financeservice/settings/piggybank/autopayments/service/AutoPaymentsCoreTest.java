package com.finovara.financeservice.settings.piggybank.autopayments.service;

import com.finovara.contracts.event.activity.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.financeservice.wallet.model.Wallet;
import org.springframework.kafka.core.KafkaTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AutoPaymentsCoreTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private AutoPaymentsCore autoPaymentsCore;

    private static final Long USER_ID = 1L;

    private PiggyBank piggyBank;
    private Wallet wallet;

    @BeforeEach
    void setup() {
        piggyBank = new PiggyBank();
        piggyBank.setAmount(BigDecimal.valueOf(100));

        wallet = Wallet.create(1L);
        wallet.deposit(BigDecimal.valueOf(500));
    }


    @Test
    void shouldApplyFullAmount() {
        autoPaymentsCore.process(USER_ID, piggyBank, wallet, BigDecimal.valueOf(200), PiggyBankAutomationMode.APPLY);

        assertThat(piggyBank.getAmount()).isEqualByComparingTo("300");
        assertThat(wallet.getBalance()).isEqualByComparingTo("300");

        ArgumentCaptor<PiggyBankActivityEvent> eventCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
        verify(kafkaTemplate).send(eq("activity.piggybank"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING);
    }

    @Test
    void shouldApplyOnlyAvailableBalance() {
        wallet.withdraw(BigDecimal.valueOf(450));

        autoPaymentsCore.process(USER_ID, piggyBank, wallet, BigDecimal.valueOf(200), PiggyBankAutomationMode.APPLY);

        assertThat(piggyBank.getAmount()).isEqualByComparingTo("150");
        assertThat(wallet.getBalance()).isEqualByComparingTo("0");

        ArgumentCaptor<PiggyBankActivityEvent> eventCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
        verify(kafkaTemplate).send(eq("activity.piggybank"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING);
    }

    @Test
    void shouldApplyZeroWhenWalletEmpty() {
        wallet.withdraw(BigDecimal.valueOf(500));

        autoPaymentsCore.process(USER_ID, piggyBank, wallet, BigDecimal.valueOf(200), PiggyBankAutomationMode.APPLY);

        assertThat(piggyBank.getAmount()).isEqualByComparingTo("100");
        assertThat(wallet.getBalance()).isEqualByComparingTo("0");

        ArgumentCaptor<PiggyBankActivityEvent> eventCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
        verify(kafkaTemplate).send(eq("activity.piggybank"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING);
    }


    @Test
    void shouldRollbackFullAmount() {
        autoPaymentsCore.process(USER_ID, piggyBank, wallet, BigDecimal.valueOf(50), PiggyBankAutomationMode.ROLLBACK);

        assertThat(piggyBank.getAmount()).isEqualByComparingTo("50");
        assertThat(wallet.getBalance()).isEqualByComparingTo("550");

        ArgumentCaptor<PiggyBankActivityEvent> eventCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
        verify(kafkaTemplate).send(eq("activity.piggybank"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING);
    }

    @Test
    void shouldRollbackOnlyAvailableInPiggyBank() {
        autoPaymentsCore.process(USER_ID, piggyBank, wallet, BigDecimal.valueOf(200), PiggyBankAutomationMode.ROLLBACK);

        assertThat(piggyBank.getAmount()).isEqualByComparingTo("0");
        assertThat(wallet.getBalance()).isEqualByComparingTo("600");

        ArgumentCaptor<PiggyBankActivityEvent> eventCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
        verify(kafkaTemplate).send(eq("activity.piggybank"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING);
    }

    @Test
    void shouldRollbackZeroWhenPiggyBankEmpty() {
        piggyBank.setAmount(BigDecimal.ZERO);

        autoPaymentsCore.process(USER_ID, piggyBank, wallet, BigDecimal.valueOf(200), PiggyBankAutomationMode.ROLLBACK);

        assertThat(piggyBank.getAmount()).isEqualByComparingTo("0");
        assertThat(wallet.getBalance()).isEqualByComparingTo("500");

        ArgumentCaptor<PiggyBankActivityEvent> eventCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
        verify(kafkaTemplate).send(eq("activity.piggybank"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING);
    }
}