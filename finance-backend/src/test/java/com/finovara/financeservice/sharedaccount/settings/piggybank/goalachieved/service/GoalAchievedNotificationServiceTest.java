package com.finovara.financeservice.sharedaccount.settings.piggybank.goalachieved.service;

import com.finovara.contracts.finance.event.sharedaccount.GoalAchievedNotificationEvent;
import com.finovara.contracts.model.transaction.PiggyBankGoalType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.sharedaccount.piggybank.model.SharedPiggyBank;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettings;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettingsRepository;
import com.finovara.financeservice.sharedaccount.settings.piggybank.goalachieved.dto.GoalAchievedNotificationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalAchievedNotificationServiceTest {

    @Mock
    private SharedAccountSettingsRepository sharedAccountSettingsRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private GoalAchievedNotificationService goalAchievedNotificationService;

    private Long userId;
    private SharedAccountSettings settings;
    private SharedPiggyBank piggyBank;

    @BeforeEach
    void setUp() {
        userId = 1L;
        settings = SharedAccountSettings.builder().id(10L).ownerId(1L).memberId(2L).piggyBankGoalAchievedNotificationEnabled(true).build();
        piggyBank = SharedPiggyBank.builder().id(20L).ownerId(1L).memberId(2L).goalType(PiggyBankGoalType.OTHER).amount(new BigDecimal("100.00")).goalAmount(new BigDecimal("100.00")).goalAchievedNotified(false).build();
    }

    @Nested
    class SaveGoalAchievedNotification {

        @Test
        void shouldUpdateNotificationFlagWhenDtoIsValid() {
            GoalAchievedNotificationDto dto = new GoalAchievedNotificationDto(Boolean.FALSE);
            when(sharedAccountSettingsRepository.findByUserId(userId)).thenReturn(settings);

            goalAchievedNotificationService.saveGoalAchievedNotification(userId, dto);

            assertFalse(settings.isPiggyBankGoalAchievedNotificationEnabled());
            verify(sharedAccountSettingsRepository).findByUserId(userId);
            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    class GetGoalAchievedNotification {

        @Test
        void shouldReturnNotificationSettingsWhenSettingsExist() {
            when(sharedAccountSettingsRepository.findByUserId(userId)).thenReturn(settings);

            GoalAchievedNotificationDto result = goalAchievedNotificationService.getGoalAchievedNotification(userId);

            assertTrue(result.piggyBankGoalAchievedNotificationEnabled());
            verify(sharedAccountSettingsRepository).findByUserId(userId);
            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    class HandleGoalAchieved {

        @Test
        void shouldCreateOutboxEventWhenGoalIsCompleted() {
            when(sharedAccountSettingsRepository.findByUserId(userId)).thenReturn(settings);

            goalAchievedNotificationService.handleGoalAchieved(userId, piggyBank);

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(outboxService).save(eq("PiggyBank"), eq("1"), eq("notification.shared-account.piggy-bank-goal-achieved"), captor.capture());
            verify(sharedAccountSettingsRepository).findByUserId(userId);
            assertTrue(piggyBank.isGoalAchievedNotified());

            GoalAchievedNotificationEvent event = (GoalAchievedNotificationEvent) captor.getValue();
            assertEquals(1L, event.ownerId());
            assertEquals(2L, event.memberId());
            assertEquals(userId, event.triggeredByUserId());
            assertEquals(20L, event.piggyBankId());
            assertEquals(new BigDecimal("100.00"), event.currentAmount());
            assertEquals(new BigDecimal("100.00"), event.goalAmount());
            assertNotNull(event.occurredAt());
        }

        @Test
        void shouldNotCreateOutboxEventWhenNotificationIsDisabled() {
            settings.setPiggyBankGoalAchievedNotificationEnabled(false);
            when(sharedAccountSettingsRepository.findByUserId(userId)).thenReturn(settings);

            goalAchievedNotificationService.handleGoalAchieved(userId, piggyBank);

            verify(sharedAccountSettingsRepository).findByUserId(userId);
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldNotCreateOutboxEventWhenGoalWasAlreadyNotified() {
            piggyBank.setGoalAchievedNotified(true);
            when(sharedAccountSettingsRepository.findByUserId(userId)).thenReturn(settings);

            goalAchievedNotificationService.handleGoalAchieved(userId, piggyBank);

            verify(sharedAccountSettingsRepository).findByUserId(userId);
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldNotCreateOutboxEventWhenGoalIsNotCompleted() {
            piggyBank.setAmount(new BigDecimal("50.00"));
            when(sharedAccountSettingsRepository.findByUserId(userId)).thenReturn(settings);

            goalAchievedNotificationService.handleGoalAchieved(userId, piggyBank);

            verify(sharedAccountSettingsRepository).findByUserId(userId);
            verify(outboxService, never()).save(any(), any(), any(), any());
        }

        @Test
        void shouldNotCreateOutboxEventWhenGoalAmountIsMissing() {
            piggyBank.setGoalAmount(null);
            when(sharedAccountSettingsRepository.findByUserId(userId)).thenReturn(settings);

            goalAchievedNotificationService.handleGoalAchieved(userId, piggyBank);

            verify(sharedAccountSettingsRepository).findByUserId(userId);
            verifyNoInteractions(outboxService);
        }
    }
}


