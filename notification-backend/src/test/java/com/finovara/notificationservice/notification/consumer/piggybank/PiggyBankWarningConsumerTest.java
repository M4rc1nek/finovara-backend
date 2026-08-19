package com.finovara.notificationservice.notification.consumer.piggybank;

import com.finovara.contracts.notification.event.piggybank.PiggyBankProgressEvent;
import com.finovara.contracts.model.NotificationType;
import com.finovara.contracts.model.transaction.PiggyBankGoalType;
import com.finovara.notificationservice.notification.dto.piggybank.PiggyBankWarningDto;
import com.finovara.notificationservice.notification.NotificationPersistenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PiggyBankWarningConsumerTest {

    @Mock
    private NotificationPersistenceService notificationPersistenceService;

    @InjectMocks
    private PiggyBankWarningConsumer piggyBankWarningConsumer;

    @Test
    void shouldSaveNotificationWhenPercentageIsExactly75() {
        PiggyBankProgressEvent event = new PiggyBankProgressEvent(1L, 5L, BigDecimal.valueOf(75), PiggyBankGoalType.VACATION, "Wakacje");

        piggyBankWarningConsumer.handle(event);

        ArgumentCaptor<PiggyBankWarningDto> captor = ArgumentCaptor.forClass(PiggyBankWarningDto.class);
        verify(notificationPersistenceService).save(eq(1L), captor.capture());

        PiggyBankWarningDto dto = captor.getValue();
        assertThat(dto.type()).isEqualTo(NotificationType.PIGGY_BANK_GOAL_APPROACHING);
        assertThat(dto.piggyBankId()).isEqualTo(5L);
        assertThat(dto.piggyBankName()).isEqualTo("Wakacje");
        assertThat(dto.goalType()).isEqualTo(PiggyBankGoalType.VACATION);
        assertThat(dto.threshold()).isEqualByComparingTo(BigDecimal.valueOf(75));
    }

    @Test
    void shouldSaveNotificationWhenPercentageIsBetween75And100() {
        PiggyBankProgressEvent event = new PiggyBankProgressEvent(1L, 5L, BigDecimal.valueOf(90), PiggyBankGoalType.VACATION, "Wakacje");

        piggyBankWarningConsumer.handle(event);

        ArgumentCaptor<PiggyBankWarningDto> captor = ArgumentCaptor.forClass(PiggyBankWarningDto.class);
        verify(notificationPersistenceService).save(eq(1L), captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.PIGGY_BANK_GOAL_APPROACHING);
    }

    @Test
    void shouldNotSaveWhenPercentageIsBelow75() {
        PiggyBankProgressEvent event = new PiggyBankProgressEvent(1L, 5L, BigDecimal.valueOf(74.99), PiggyBankGoalType.VACATION, "Wakacje");

        piggyBankWarningConsumer.handle(event);

        verify(notificationPersistenceService, never()).save(any(), any());
    }

    @Test
    void shouldNotSaveWhenPercentageIsExactly100() {
        PiggyBankProgressEvent event = new PiggyBankProgressEvent(1L, 5L, BigDecimal.valueOf(100), PiggyBankGoalType.VACATION, "Wakacje");

        piggyBankWarningConsumer.handle(event);

        verify(notificationPersistenceService, never()).save(any(), any());
    }

    @Test
    void shouldNotSaveWhenPercentageIsAbove100() {
        PiggyBankProgressEvent event = new PiggyBankProgressEvent(1L, 5L, BigDecimal.valueOf(120), PiggyBankGoalType.VACATION, "Wakacje");

        piggyBankWarningConsumer.handle(event);

        verify(notificationPersistenceService, never()).save(any(), any());
    }
}