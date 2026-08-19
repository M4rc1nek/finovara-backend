package com.finovara.notificationservice.notification.consumer.limit;

import com.finovara.contracts.notification.event.limit.LimitStatsEvent;
import com.finovara.contracts.model.NotificationType;
import com.finovara.contracts.model.PeriodType;
import com.finovara.notificationservice.notification.dto.limit.LimitWarningDto;
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
class LimitWarningConsumerTest {

    @Mock
    private NotificationPersistenceService notificationPersistenceService;

    @InjectMocks
    private LimitWarningConsumer limitWarningConsumer;

    @Test
    void shouldSaveNotificationWhenPercentageIsExactly75() {
        LimitStatsEvent event = new LimitStatsEvent(1L, 10L, BigDecimal.valueOf(75), PeriodType.WEEKLY);

        limitWarningConsumer.handle(event);

        ArgumentCaptor<LimitWarningDto> captor = ArgumentCaptor.forClass(LimitWarningDto.class);
        verify(notificationPersistenceService).save(eq(1L), captor.capture());

        LimitWarningDto dto = captor.getValue();
        assertThat(dto.type()).isEqualTo(NotificationType.LIMIT_EXCEEDED_WARNING);
        assertThat(dto.limitId()).isEqualTo(10L);
        assertThat(dto.period()).isEqualTo(PeriodType.WEEKLY);
        assertThat(dto.limitPercentage()).isEqualByComparingTo(BigDecimal.valueOf(75));
        assertThat(dto.threshold()).isEqualByComparingTo(BigDecimal.valueOf(75));
    }

    @Test
    void shouldSaveNotificationWhenPercentageIsBetween75And100() {
        LimitStatsEvent event = new LimitStatsEvent(1L, 10L, BigDecimal.valueOf(90), PeriodType.MONTHLY);

        limitWarningConsumer.handle(event);

        ArgumentCaptor<LimitWarningDto> captor = ArgumentCaptor.forClass(LimitWarningDto.class);
        verify(notificationPersistenceService).save(eq(1L), captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.LIMIT_EXCEEDED_WARNING);
    }

    @Test
    void shouldNotSaveWhenPercentageIsBelow75() {
        LimitStatsEvent event = new LimitStatsEvent(1L, 10L, BigDecimal.valueOf(74.99), PeriodType.WEEKLY);

        limitWarningConsumer.handle(event);

        verify(notificationPersistenceService, never()).save(any(), any());
    }

    @Test
    void shouldNotSaveWhenPercentageIsExactly100() {
        LimitStatsEvent event = new LimitStatsEvent(1L, 10L, BigDecimal.valueOf(100), PeriodType.WEEKLY);

        limitWarningConsumer.handle(event);

        verify(notificationPersistenceService, never()).save(any(), any());
    }

    @Test
    void shouldNotSaveWhenPercentageIsAbove100() {
        LimitStatsEvent event = new LimitStatsEvent(1L, 10L, BigDecimal.valueOf(120), PeriodType.WEEKLY);

        limitWarningConsumer.handle(event);

        verify(notificationPersistenceService, never()).save(any(), any());
    }
}