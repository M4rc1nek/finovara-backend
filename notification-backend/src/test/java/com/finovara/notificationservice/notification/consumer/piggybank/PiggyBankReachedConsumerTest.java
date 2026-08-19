package com.finovara.notificationservice.notification.consumer.piggybank;

import com.finovara.contracts.notification.event.piggybank.PiggyBankProgressEvent;
import com.finovara.contracts.model.NotificationType;
import com.finovara.contracts.model.transaction.PiggyBankGoalType;
import com.finovara.notificationservice.notification.dto.piggybank.PiggyBankReachedDto;
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
class PiggyBankReachedConsumerTest {

    @Mock
    private NotificationPersistenceService notificationPersistenceService;

    @InjectMocks
    private PiggyBankReachedConsumer piggyBankReachedConsumer;

    @Test
    void shouldSaveNotificationWhenPercentageIsExactly100() {
        PiggyBankProgressEvent event = new PiggyBankProgressEvent(1L, 5L, BigDecimal.valueOf(100), PiggyBankGoalType.VACATION, "Wakacje");

        piggyBankReachedConsumer.handle(event);

        ArgumentCaptor<PiggyBankReachedDto> captor = ArgumentCaptor.forClass(PiggyBankReachedDto.class);
        verify(notificationPersistenceService).save(eq(1L), captor.capture());

        PiggyBankReachedDto dto = captor.getValue();
        assertThat(dto.type()).isEqualTo(NotificationType.PIGGY_BANK_GOAL_REACHED);
        assertThat(dto.piggyBankId()).isEqualTo(5L);
        assertThat(dto.piggyBankName()).isEqualTo("Wakacje");
        assertThat(dto.goalType()).isEqualTo(PiggyBankGoalType.VACATION);
        assertThat(dto.threshold()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void shouldSaveNotificationWhenPercentageIsAbove100() {
        PiggyBankProgressEvent event = new PiggyBankProgressEvent(1L, 5L, BigDecimal.valueOf(110), PiggyBankGoalType.VACATION, "Wakacje");

        piggyBankReachedConsumer.handle(event);

        ArgumentCaptor<PiggyBankReachedDto> captor = ArgumentCaptor.forClass(PiggyBankReachedDto.class);
        verify(notificationPersistenceService).save(eq(1L), captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.PIGGY_BANK_GOAL_REACHED);
    }

    @Test
    void shouldNotSaveWhenPercentageIsBelow100() {
        PiggyBankProgressEvent event = new PiggyBankProgressEvent(1L, 5L, BigDecimal.valueOf(99.99), PiggyBankGoalType.VACATION, "Wakacje");

        piggyBankReachedConsumer.handle(event);

        verify(notificationPersistenceService, never()).save(any(), any());
    }

    @Test
    void shouldNotSaveWhenPercentageIsZero() {
        PiggyBankProgressEvent event = new PiggyBankProgressEvent(1L, 5L, BigDecimal.ZERO, PiggyBankGoalType.VACATION, "Wakacje");

        piggyBankReachedConsumer.handle(event);

        verify(notificationPersistenceService, never()).save(any(), any());
    }
}