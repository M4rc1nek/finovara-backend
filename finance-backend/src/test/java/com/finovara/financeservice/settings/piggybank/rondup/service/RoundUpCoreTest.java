package com.finovara.financeservice.settings.piggybank.rondup.service;

import com.finovara.contracts.activity.event.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.financeservice.settings.piggybank.roundup.service.RoundUpCore;
import com.finovara.financeservice.wallet.model.Wallet;
import org.springframework.kafka.core.KafkaTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoundUpCoreTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private RoundUpCore roundUpCore;

    private static final Long USER_ID = 1L;

    @Test
    void shouldApplyRoundUpSuccessfully() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("10.00"));

        Wallet wallet = Wallet.create(1L);
        wallet.deposit(new BigDecimal("50.00"));

        BigDecimal roundUp = new BigDecimal("5.00");

        roundUpCore.process(USER_ID, piggyBank, wallet, roundUp, PiggyBankAutomationMode.APPLY);

        assertEquals(new BigDecimal("15.00"), piggyBank.getAmount());
        assertEquals(new BigDecimal("45.00"), wallet.getBalance());

        ArgumentCaptor<PiggyBankActivityEvent> eventCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
        verify(kafkaTemplate).send(eq("activity.piggybank"), eventCaptor.capture());
        assertEquals(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, eventCaptor.getValue().type());
    }

    @Test
    void shouldRollbackSuccessfully() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("20.00"));

        Wallet wallet = Wallet.create(1L);
        wallet.deposit(new BigDecimal("10.00"));

        BigDecimal roundUp = new BigDecimal("5.00");

        roundUpCore.process(USER_ID, piggyBank, wallet, roundUp, PiggyBankAutomationMode.ROLLBACK);

        assertEquals(new BigDecimal("15.00"), piggyBank.getAmount());
        assertEquals(new BigDecimal("15.00"), wallet.getBalance());

        ArgumentCaptor<PiggyBankActivityEvent> eventCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
        verify(kafkaTemplate).send(eq("activity.piggybank"), eventCaptor.capture());
        assertEquals(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, eventCaptor.getValue().type());
    }

    @Test
    void shouldRollbackOnlyAvailableAmount() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("3.00"));

        Wallet wallet = Wallet.create(1L);
        wallet.deposit(new BigDecimal("10.00"));

        BigDecimal roundUp = new BigDecimal("5.00");

        roundUpCore.process(USER_ID, piggyBank, wallet, roundUp, PiggyBankAutomationMode.ROLLBACK);

        assertThat(piggyBank.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(13));

        ArgumentCaptor<PiggyBankActivityEvent> eventCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
        verify(kafkaTemplate).send(eq("activity.piggybank"), eventCaptor.capture());
        assertEquals(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, eventCaptor.getValue().type());
    }

    @Test
    void shouldDoNothingWhenRoundUpIsZeroOrNegative() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("10.00"));

        Wallet wallet = Wallet.create(1L);
        wallet.deposit(new BigDecimal("50.00"));

        roundUpCore.process(USER_ID, piggyBank, wallet, BigDecimal.ZERO, PiggyBankAutomationMode.APPLY);

        assertEquals(new BigDecimal("10.00"), piggyBank.getAmount());
        assertEquals(new BigDecimal("50.00"), wallet.getBalance());

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldDoNothingWhenRollbackAmountIsZero() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(BigDecimal.ZERO);

        Wallet wallet = Wallet.create(1L);
        wallet.deposit(new BigDecimal("10.00"));

        roundUpCore.process(USER_ID, piggyBank, wallet, new BigDecimal("5.00"), PiggyBankAutomationMode.ROLLBACK);

        assertEquals(BigDecimal.ZERO, piggyBank.getAmount());
        assertEquals(new BigDecimal("10.00"), wallet.getBalance());

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldThrowWhenInsufficientFunds() {
        PiggyBank piggyBank = new PiggyBank();
        piggyBank.setAmount(new BigDecimal("10.00"));

        Wallet wallet = Wallet.create(1L);
        wallet.deposit(new BigDecimal("2.00"));

        BigDecimal roundUp = new BigDecimal("5.00");

        assertThrows(InvalidInputException.class, () -> roundUpCore.process(USER_ID, piggyBank, wallet, roundUp, PiggyBankAutomationMode.APPLY));

        verifyNoInteractions(kafkaTemplate);
    }
}