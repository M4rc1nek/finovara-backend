package com.finovara.financeservice.settings.piggybank.completion.service;

import com.finovara.contracts.activity.event.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.financeservice.settings.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.financeservice.wallet.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalCompletionCoreTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private OutboxService outboxService;

    @Mock
    private RecurringSettingsRepository recurringSettingsRepository;

    @Mock
    private PiggyBankRepository piggyBankRepository;

    @InjectMocks
    private GoalCompletionCore goalCompletionCore;

    private PiggyBank piggyBank;
    private Wallet wallet;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setup() {
        piggyBank = new PiggyBank();
        piggyBank.setId(10L);
        piggyBank.setAmount(BigDecimal.valueOf(200));

        wallet = Wallet.create(USER_ID);
        wallet.deposit(BigDecimal.valueOf(500));
    }

    @Test
    void shouldDoNothingForNoneStrategy() {
        goalCompletionCore.apply(USER_ID, piggyBank, wallet, GoalCompletionStrategy.NONE);

        assertEquals(BigDecimal.valueOf(500), wallet.getBalance());
        assertEquals(BigDecimal.valueOf(200), piggyBank.getAmount());

        verifyNoInteractions(kafkaTemplate, outboxService, recurringSettingsRepository, piggyBankRepository);
    }

    @Test
    void shouldTransferMoneyAndKeepPiggyBank() {
        goalCompletionCore.apply(USER_ID, piggyBank, wallet, GoalCompletionStrategy.WITHDRAW_AND_KEEP);

        assertEquals(BigDecimal.valueOf(700), wallet.getBalance());
        assertEquals(BigDecimal.ZERO, piggyBank.getAmount());

        ArgumentCaptor<PiggyBankActivityEvent> eventCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
        verify(kafkaTemplate).send(eq("activity.piggybank"), eventCaptor.capture());
        assertEquals(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, eventCaptor.getValue().type());

        verifyNoInteractions(outboxService, recurringSettingsRepository, piggyBankRepository);
    }

    @Test
    void shouldTransferMoneyAndDeletePiggyBank() {
        when(recurringSettingsRepository.findByUserIdAndPiggyBankId(USER_ID, piggyBank.getId()))
                .thenReturn(Optional.empty());

        goalCompletionCore.apply(USER_ID, piggyBank, wallet, GoalCompletionStrategy.WITHDRAW_AND_DELETE);

        assertEquals(BigDecimal.valueOf(700), wallet.getBalance());
        assertEquals(BigDecimal.ZERO, piggyBank.getAmount());

        ArgumentCaptor<PiggyBankActivityEvent> kafkaCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
        verify(kafkaTemplate).send(eq("activity.piggybank"), kafkaCaptor.capture());
        assertEquals(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, kafkaCaptor.getValue().type());

        ArgumentCaptor<Object> outboxCaptor = ArgumentCaptor.forClass(Object.class);
        verify(outboxService).save(
                eq("SharedPiggyBank"),
                eq(piggyBank.getId().toString()),
                eq("activity.piggybank"),
                outboxCaptor.capture()
        );
        PiggyBankActivityEvent deletedEvent = (PiggyBankActivityEvent) outboxCaptor.getValue();
        assertEquals(PiggyBankActivityType.DELETED_PIGGY_BANK, deletedEvent.type());

        verify(recurringSettingsRepository).findByUserIdAndPiggyBankId(USER_ID, piggyBank.getId());
        verify(piggyBankRepository).delete(piggyBank);
    }

    @Test
    void shouldDisableRecurringSavingsWhenDeletingPiggyBank() {
        RecurringSettings settings = RecurringSettings.builder()
                .enable(true)
                .nextExecutionDate(LocalDate.now())
                .build();

        when(recurringSettingsRepository.findByUserIdAndPiggyBankId(USER_ID, piggyBank.getId()))
                .thenReturn(Optional.of(settings));

        goalCompletionCore.apply(USER_ID, piggyBank, wallet, GoalCompletionStrategy.WITHDRAW_AND_DELETE);

        assertFalse(settings.isEnable());
        assertNull(settings.getNextExecutionDate());
        assertNull(settings.getPiggyBankId());
    }

    @Test
    void shouldNotTransferWhenAmountIsZero() {
        piggyBank.setAmount(BigDecimal.ZERO);

        goalCompletionCore.apply(USER_ID, piggyBank, wallet, GoalCompletionStrategy.WITHDRAW_AND_KEEP);

        assertEquals(BigDecimal.valueOf(500), wallet.getBalance());
        assertEquals(BigDecimal.ZERO, piggyBank.getAmount());

        ArgumentCaptor<PiggyBankActivityEvent> eventCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
        verify(kafkaTemplate).send(eq("activity.piggybank"), eventCaptor.capture());
        assertEquals(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, eventCaptor.getValue().type());
    }

    @Test
    void shouldNotTransferWhenAmountIsNull() {
        piggyBank.setAmount(null);

        goalCompletionCore.apply(USER_ID, piggyBank, wallet, GoalCompletionStrategy.WITHDRAW_AND_KEEP);

        assertEquals(BigDecimal.valueOf(500), wallet.getBalance());
        assertNull(piggyBank.getAmount());

        ArgumentCaptor<PiggyBankActivityEvent> eventCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
        verify(kafkaTemplate).send(eq("activity.piggybank"), eventCaptor.capture());
        assertEquals(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, eventCaptor.getValue().type());
    }
}