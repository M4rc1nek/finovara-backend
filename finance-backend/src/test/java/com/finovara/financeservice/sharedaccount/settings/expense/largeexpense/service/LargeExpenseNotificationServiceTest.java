package com.finovara.financeservice.sharedaccount.settings.expense.largeexpense.service;

import com.finovara.contracts.finance.event.sharedaccount.LargeExpenseNotificationEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettings;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettingsRepository;
import com.finovara.financeservice.sharedaccount.settings.expense.largeexpense.dto.LargeExpenseNotificationDto;
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
class LargeExpenseNotificationServiceTest {

    @Mock
    private SharedAccountSettingsRepository sharedAccountSettingsRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private LargeExpenseNotificationService largeExpenseNotificationService;

    private Long userId;
    private SharedAccountSettings settings;
    private SharedExpense expense;

    @BeforeEach
    void setUp() {
        userId = 1L;
        settings = SharedAccountSettings.builder().id(10L).ownerId(1L).memberId(2L).largeExpenseNotificationEnabled(true).largeExpenseNotificationThreshold(new BigDecimal("100.00")).build();
        expense = SharedExpense.builder().id(20L).ownerId(1L).memberId(2L).amount(new BigDecimal("150.00")).build();
    }

    @Nested
    class SaveLargeExpenseNotification {

        @Test
        void shouldUpdateNotificationSettingsWhenDtoIsValid() {
            LargeExpenseNotificationDto dto = new LargeExpenseNotificationDto(Boolean.TRUE, new BigDecimal("250.00"));
            when(sharedAccountSettingsRepository.findByUserId(userId)).thenReturn(settings);

            largeExpenseNotificationService.saveLargeExpenseNotification(userId, dto);

            assertTrue(settings.isLargeExpenseNotificationEnabled());
            assertEquals(new BigDecimal("250.00"), settings.getLargeExpenseNotificationThreshold());
            verify(sharedAccountSettingsRepository).findByUserId(userId);
            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    class GetLargeExpenseNotification {

        @Test
        void shouldReturnNotificationSettingsWhenSettingsExist() {
            when(sharedAccountSettingsRepository.findByUserId(userId)).thenReturn(settings);

            LargeExpenseNotificationDto result = largeExpenseNotificationService.getLargeExpenseNotification(userId);

            assertEquals(Boolean.TRUE, result.largeExpenseNotificationEnabled());
            assertEquals(new BigDecimal("100.00"), result.largeExpenseNotificationThreshold());
            verify(sharedAccountSettingsRepository).findByUserId(userId);
            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    class HandleLargeNotification {

        @Test
        void shouldCreateOutboxEventWhenAmountExceedsThreshold() {
            when(sharedAccountSettingsRepository.findByUserId(userId)).thenReturn(settings);

            largeExpenseNotificationService.handleLargeNotification(userId, expense);

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(outboxService).save(eq("SharedExpense"), eq("20"), eq("notification.shared-account.large-expense-detected"), captor.capture());
            verify(sharedAccountSettingsRepository).findByUserId(userId);

            LargeExpenseNotificationEvent event = (LargeExpenseNotificationEvent) captor.getValue();
            assertEquals(1L, event.ownerId());
            assertEquals(2L, event.memberId());
            assertEquals(userId, event.triggeredByUserId());
            assertEquals(20L, event.expenseId());
            assertEquals(new BigDecimal("150.00"), event.amount());
            assertEquals(new BigDecimal("100.00"), event.threshold());
            assertNotNull(event.occurredAt());
        }

        @Test
        void shouldNotCreateOutboxEventWhenNotificationIsDisabled() {
            settings.setLargeExpenseNotificationEnabled(false);
            when(sharedAccountSettingsRepository.findByUserId(userId)).thenReturn(settings);

            largeExpenseNotificationService.handleLargeNotification(userId, expense);

            verify(sharedAccountSettingsRepository).findByUserId(userId);
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldNotCreateOutboxEventWhenThresholdIsMissing() {
            settings.setLargeExpenseNotificationThreshold(null);
            when(sharedAccountSettingsRepository.findByUserId(userId)).thenReturn(settings);

            largeExpenseNotificationService.handleLargeNotification(userId, expense);

            verify(sharedAccountSettingsRepository).findByUserId(userId);
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldNotCreateOutboxEventWhenAmountDoesNotExceedThreshold() {
            expense.setAmount(new BigDecimal("100.00"));
            when(sharedAccountSettingsRepository.findByUserId(userId)).thenReturn(settings);

            largeExpenseNotificationService.handleLargeNotification(userId, expense);

            verify(sharedAccountSettingsRepository).findByUserId(userId);
            verify(outboxService, never()).save(any(), any(), any(), any());
        }
    }
}


