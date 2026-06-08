package com.finovara.corebackend.usersetting.piggybank.completion.service;

import com.finovara.contracts.event.activity.piggybank.PiggyBankActivityEvent;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.corebackend.piggybank.model.PiggyBank;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.corebackend.usersetting.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.corebackend.usersetting.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.corebackend.wallet.model.Wallet;
import org.springframework.kafka.core.KafkaTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalCompletionCoreTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private RecurringSettingsRepository recurringSettingsRepository;

    @InjectMocks
    private GoalCompletionCore goalCompletionCore;

    private PiggyBank piggyBank;
    private Wallet wallet;
    private User user;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setup() {
        piggyBank = new PiggyBank();
        piggyBank.setId(10L);
        piggyBank.setAmount(BigDecimal.valueOf(200));

        user = new User();
        user.setId(USER_ID);
        user.setPiggyBanks(new ArrayList<>());
        user.getPiggyBanks().add(piggyBank);

        wallet = Wallet.create(user);
        wallet.deposit(BigDecimal.valueOf(500));
    }

    @Test
    void shouldDoNothingForNoneStrategy() {
        goalCompletionCore.apply(USER_ID, piggyBank, wallet, user, GoalCompletionStrategy.NONE);

        assertEquals(BigDecimal.valueOf(500), wallet.getBalance());
        assertEquals(BigDecimal.valueOf(200), piggyBank.getAmount());

        verifyNoInteractions(kafkaTemplate, recurringSettingsRepository);
    }

    @Test
    void shouldTransferMoneyAndKeepPiggyBank() {
        goalCompletionCore.apply(USER_ID, piggyBank, wallet, user, GoalCompletionStrategy.WITHDRAW_AND_KEEP);

        assertEquals(BigDecimal.valueOf(700), wallet.getBalance());
        assertEquals(BigDecimal.ZERO, piggyBank.getAmount());

        ArgumentCaptor<PiggyBankActivityEvent> eventCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
        verify(kafkaTemplate, times(1)).send(eq("activity.piggybank"), eventCaptor.capture());
        assertEquals(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, eventCaptor.getValue().type());
        verifyNoInteractions(recurringSettingsRepository);

        assertTrue(user.getPiggyBanks().contains(piggyBank));
    }

    @Test
    void shouldTransferMoneyAndDeletePiggyBank() {
        when(recurringSettingsRepository.findByUserAssignedIdAndPiggyBankId(USER_ID, piggyBank.getId()))
                .thenReturn(Optional.empty());

        goalCompletionCore.apply(USER_ID, piggyBank, wallet, user, GoalCompletionStrategy.WITHDRAW_AND_DELETE);

        assertEquals(BigDecimal.valueOf(700), wallet.getBalance());
        assertEquals(BigDecimal.ZERO, piggyBank.getAmount());

        verify(kafkaTemplate, times(2)).send(eq("activity.piggybank"), any(PiggyBankActivityEvent.class));

        verify(recurringSettingsRepository).findByUserAssignedIdAndPiggyBankId(USER_ID, piggyBank.getId());

        assertFalse(user.getPiggyBanks().contains(piggyBank));
    }

    @Test
    void shouldDisableRecurringSavingsWhenDeletingPiggyBank() {
        RecurringSettings settings = RecurringSettings.builder()
                .enable(true)
                .nextExecutionDate(LocalDate.now())
                .build();

        when(recurringSettingsRepository.findByUserAssignedIdAndPiggyBankId(USER_ID, piggyBank.getId()))
                .thenReturn(Optional.of(settings));

        goalCompletionCore.apply(USER_ID, piggyBank, wallet, user, GoalCompletionStrategy.WITHDRAW_AND_DELETE);

        assertFalse(settings.isEnable());
        assertNull(settings.getNextExecutionDate());
        assertNull(settings.getPiggyBankId());
    }


    @Test
    void shouldNotTransferWhenAmountIsZero() {
        piggyBank.setAmount(BigDecimal.ZERO);

        goalCompletionCore.apply(USER_ID, piggyBank, wallet, user, GoalCompletionStrategy.WITHDRAW_AND_KEEP);

        assertEquals(BigDecimal.valueOf(500), wallet.getBalance());
        assertEquals(BigDecimal.ZERO, piggyBank.getAmount());

        ArgumentCaptor<PiggyBankActivityEvent> eventCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
        verify(kafkaTemplate).send(eq("activity.piggybank"), eventCaptor.capture());
        assertEquals(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, eventCaptor.getValue().type());
    }

    @Test
    void shouldNotTransferWhenAmountIsNull() {
        piggyBank.setAmount(null);

        goalCompletionCore.apply(USER_ID, piggyBank, wallet, user, GoalCompletionStrategy.WITHDRAW_AND_KEEP);

        assertEquals(BigDecimal.valueOf(500), wallet.getBalance());
        assertNull(piggyBank.getAmount());

        ArgumentCaptor<PiggyBankActivityEvent> eventCaptor = ArgumentCaptor.forClass(PiggyBankActivityEvent.class);
        verify(kafkaTemplate).send(eq("activity.piggybank"), eventCaptor.capture());
        assertEquals(PiggyBankActivityType.AMOUNT_REMOVED_FROM_PIGGY_BANK_BY_SETTING, eventCaptor.getValue().type());
    }
}
