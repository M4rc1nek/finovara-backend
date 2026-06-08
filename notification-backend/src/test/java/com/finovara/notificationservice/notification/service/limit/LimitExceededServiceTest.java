package com.finovara.notificationservice.notification.service.limit;

import com.finovara.contracts.event.notification.limit.LimitStatsEvent;
import com.finovara.contracts.model.NotificationType;
import com.finovara.contracts.model.PeriodType;
import com.finovara.notificationservice.notification.dto.limit.LimitExceededDto;
import com.finovara.notificationservice.notification.service.NotificationPersistenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class LimitExceededServiceTest {

    @Mock
    private NotificationPersistenceService notificationPersistenceService;

    @InjectMocks
    private LimitExceededService limitExceededService;

    @Test
    void shouldSaveNotificationWhenPercentageIsExactly100() {
        LimitStatsEvent event = new LimitStatsEvent(1L, 10L, BigDecimal.valueOf(100), PeriodType.WEEKLY);

        limitExceededService.handle(event);

        ArgumentCaptor<LimitExceededDto> captor = ArgumentCaptor.forClass(LimitExceededDto.class);
        verify(notificationPersistenceService).save(eq(1L), captor.capture());

        LimitExceededDto dto = captor.getValue();
        assertThat(dto.type()).isEqualTo(NotificationType.LIMIT_EXCEEDED);
        assertThat(dto.limitId()).isEqualTo(10L);
        assertThat(dto.period()).isEqualTo(PeriodType.WEEKLY);
        assertThat(dto.threshold()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void shouldSaveNotificationWhenPercentageIsAbove100() {
        LimitStatsEvent event = new LimitStatsEvent(1L, 10L, BigDecimal.valueOf(150), PeriodType.MONTHLY);

        limitExceededService.handle(event);

        ArgumentCaptor<LimitExceededDto> captor = ArgumentCaptor.forClass(LimitExceededDto.class);
        verify(notificationPersistenceService).save(eq(1L), captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.LIMIT_EXCEEDED);
    }

    @Test
    void shouldNotSaveWhenPercentageIsBelow100() {
        LimitStatsEvent event = new LimitStatsEvent(1L, 10L, BigDecimal.valueOf(99.99), PeriodType.WEEKLY);

        limitExceededService.handle(event);

        verify(notificationPersistenceService, never()).save(any(), any());
    }

    @Test
    void shouldNotSaveWhenPercentageIsZero() {
        LimitStatsEvent event = new LimitStatsEvent(1L, 10L, BigDecimal.ZERO, PeriodType.WEEKLY);

        limitExceededService.handle(event);

        verify(notificationPersistenceService, never()).save(any(), any());
    }
}