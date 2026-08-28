package com.finovara.notificationservice.notification.consumer.recurring;

import com.finovara.contracts.model.NotificationType;
import com.finovara.contracts.model.RecurringType;
import com.finovara.contracts.notification.event.recurring.transaction.RecurringExecutionSkippedEvent;
import com.finovara.notificationservice.notification.NotificationPersistenceService;
import com.finovara.notificationservice.notification.dto.recurring.RecurringExecutionSkippedDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecurringExecutionSkippedConsumerTest {

    @Mock
    private NotificationPersistenceService notificationPersistenceService;

    private RecurringExecutionSkippedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new RecurringExecutionSkippedConsumer(notificationPersistenceService);
    }

    @Test
    void shouldPersistNotificationWithCorrectFields() {
        LocalDate lastScheduledDate = LocalDate.of(2026, 1, 10);
        LocalDateTime occurredAt = LocalDateTime.of(2026, 1, 10, 12, 0);

        RecurringExecutionSkippedEvent event = new RecurringExecutionSkippedEvent(
                1L, RecurringType.EXPENSE, 99L, BigDecimal.valueOf(200), lastScheduledDate, 3, occurredAt);

        consumer.handle(event);

        ArgumentCaptor<RecurringExecutionSkippedDto> captor = ArgumentCaptor.forClass(RecurringExecutionSkippedDto.class);
        verify(notificationPersistenceService).save(eq(1L), captor.capture());

        RecurringExecutionSkippedDto dto = captor.getValue();
        assertThat(dto.type()).isEqualTo(NotificationType.RECURRING_EXECUTION_SKIPPED);
        assertThat(dto.recurringType()).isEqualTo(RecurringType.EXPENSE);
        assertThat(dto.recurringSettingsId()).isEqualTo(99L);
        assertThat(dto.amount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(dto.lastScheduledDate()).isEqualTo(lastScheduledDate);
        assertThat(dto.skippedCount()).isEqualTo(3);
        assertThat(dto.createdAt()).isNotNull();
    }

    @Test
    void shouldPersistNotificationForUserIdFromEvent() {
        RecurringExecutionSkippedEvent event = new RecurringExecutionSkippedEvent(
                42L, RecurringType.SAVINGS, 5L, BigDecimal.valueOf(50), LocalDate.now(), 1, LocalDateTime.now());

        consumer.handle(event);

        verify(notificationPersistenceService).save(eq(42L), any(RecurringExecutionSkippedDto.class));
    }

    @Test
    void shouldMapSkippedCountCorrectlyForMultipleSkips() {
        RecurringExecutionSkippedEvent event = new RecurringExecutionSkippedEvent(
                1L, RecurringType.REVENUE, 7L, BigDecimal.valueOf(500), LocalDate.now(), 5, LocalDateTime.now());

        consumer.handle(event);

        ArgumentCaptor<RecurringExecutionSkippedDto> captor = ArgumentCaptor.forClass(RecurringExecutionSkippedDto.class);
        verify(notificationPersistenceService).save(eq(1L), captor.capture());

        assertThat(captor.getValue().skippedCount()).isEqualTo(5);
    }
}