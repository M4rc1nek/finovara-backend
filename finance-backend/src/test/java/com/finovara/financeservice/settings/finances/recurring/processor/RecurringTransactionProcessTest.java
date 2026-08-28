package com.finovara.financeservice.settings.finances.recurring.processor;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.RecurringType;
import com.finovara.contracts.notification.event.recurring.transaction.RecurringExecutionSkippedEvent;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.financeservice.settings.finances.recurring.service.execution.RecurringExecutionResult;
import com.finovara.financeservice.settings.finances.recurring.service.execution.RecurringExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionProcessTest {

    @Mock
    private RecurringSettingsRepository recurringSettingsRepository;

    @Mock
    private RecurringExecutionService recurringExecutionService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private RecurringTransactionProcess recurringTransactionProcess;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        recurringTransactionProcess = new RecurringTransactionProcess(
                recurringSettingsRepository, recurringExecutionService, kafkaTemplate);

        today = LocalDate.of(2026, 1, 10);
    }

    private RecurringSettings createValidSettings(LocalDate nextExecutionDate) {
        RecurringSettings settings = new RecurringSettings();
        settings.setId(99L);
        settings.setEnable(true);
        settings.setUserId(1L);
        settings.setType(RecurringType.EXPENSE);
        settings.setAmount(BigDecimal.valueOf(100));
        settings.setPeriodType(PeriodType.DAILY);
        settings.setNextExecutionDate(nextExecutionDate);
        settings.setEndDate(null);
        settings.setSkippedNotificationSent(false);
        return settings;
    }

    @Nested
    class LoopExecutionTests {

        @Test
        void shouldExecuteOnceWhenDateIsToday() {
            RecurringSettings settings = createValidSettings(today);
            when(recurringExecutionService.execute(eq(settings), any())).thenReturn(RecurringExecutionResult.EXECUTED);

            recurringTransactionProcess.processSingle(settings, today);

            verify(recurringExecutionService, times(1)).execute(settings, today);
        }

        @Test
        void shouldExecuteMultipleTimesForPastDates() {
            RecurringSettings settings = createValidSettings(today.minusDays(3));
            when(recurringExecutionService.execute(eq(settings), any())).thenReturn(RecurringExecutionResult.EXECUTED);

            recurringTransactionProcess.processSingle(settings, today);

            verify(recurringExecutionService, times(4)).execute(eq(settings), any());
        }

        @Test
        void shouldStopWhenDisabledDuringExecution() {
            RecurringSettings settings = createValidSettings(today.minusDays(2));

            doAnswer(invocation -> {
                settings.setEnable(false);
                return RecurringExecutionResult.EXECUTED;
            }).when(recurringExecutionService).execute(eq(settings), any());

            recurringTransactionProcess.processSingle(settings, today);

            verify(recurringExecutionService, times(1)).execute(eq(settings), any());
        }

        @Test
        void shouldRespectMaxIterationsLimit() {
            RecurringSettings settings = createValidSettings(today.minusDays(200));
            when(recurringExecutionService.execute(eq(settings), any())).thenReturn(RecurringExecutionResult.EXECUTED);

            recurringTransactionProcess.processSingle(settings, today);

            verify(recurringExecutionService, atMost(100)).execute(eq(settings), any());
        }

        @Test
        void shouldUpdateNextExecutionDateAndSaveSettings() {
            RecurringSettings settings = createValidSettings(today.minusDays(1));
            when(recurringExecutionService.execute(eq(settings), any())).thenReturn(RecurringExecutionResult.EXECUTED);

            recurringTransactionProcess.processSingle(settings, today);

            verify(recurringSettingsRepository).save(settings);
            assertThat(settings.getNextExecutionDate()).isEqualTo(today.plusDays(1));
        }

        @Test
        void shouldSetNextExecutionDateToNullWhenDisabledDuringExecution() {
            RecurringSettings settings = createValidSettings(today);

            doAnswer(invocation -> {
                settings.setEnable(false);
                return RecurringExecutionResult.EXECUTED;
            }).when(recurringExecutionService).execute(eq(settings), any());

            recurringTransactionProcess.processSingle(settings, today);

            assertThat(settings.getNextExecutionDate()).isNull();
        }
    }

    @Nested
    class SkippedNotificationTests {

        @Test
        void shouldNotSendNotificationWhenNoExecutionsAreSkipped() {
            RecurringSettings settings = createValidSettings(today);
            when(recurringExecutionService.execute(eq(settings), any())).thenReturn(RecurringExecutionResult.EXECUTED);

            recurringTransactionProcess.processSingle(settings, today);

            verifyNoInteractions(kafkaTemplate);
            assertThat(settings.isSkippedNotificationSent()).isFalse();
        }

        @Test
        void shouldSendNotificationWhenExecutionIsSkipped() {
            RecurringSettings settings = createValidSettings(today);
            when(recurringExecutionService.execute(eq(settings), any())).thenReturn(RecurringExecutionResult.SKIPPED);

            recurringTransactionProcess.processSingle(settings, today);

            verify(kafkaTemplate).send(eq("activity.recurring.skipped"), any(RecurringExecutionSkippedEvent.class));
        }

        @Test
        void shouldSendEventWithCorrectFieldsWhenSingleSkip() {
            RecurringSettings settings = createValidSettings(today);
            when(recurringExecutionService.execute(eq(settings), any())).thenReturn(RecurringExecutionResult.SKIPPED);

            recurringTransactionProcess.processSingle(settings, today);

            ArgumentCaptor<RecurringExecutionSkippedEvent> captor = ArgumentCaptor.forClass(RecurringExecutionSkippedEvent.class);
            verify(kafkaTemplate).send(eq("activity.recurring.skipped"), captor.capture());

            RecurringExecutionSkippedEvent event = captor.getValue();
            assertThat(event.userId()).isEqualTo(1L);
            assertThat(event.type()).isEqualTo(RecurringType.EXPENSE);
            assertThat(event.recurringSettingsId()).isEqualTo(99L);
            assertThat(event.amount()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(event.lastScheduledDate()).isEqualTo(today);
            assertThat(event.skippedCount()).isEqualTo(1);
            assertThat(event.occurredAt()).isNotNull();
        }

        @Test
        void shouldAggregateSkippedCountAndUseLastSkippedDateAcrossMultipleIterations() {
            RecurringSettings settings = createValidSettings(today.minusDays(3));

            when(recurringExecutionService.execute(eq(settings), any()))
                    .thenReturn(RecurringExecutionResult.EXECUTED)
                    .thenReturn(RecurringExecutionResult.SKIPPED)
                    .thenReturn(RecurringExecutionResult.EXECUTED)
                    .thenReturn(RecurringExecutionResult.SKIPPED);

            recurringTransactionProcess.processSingle(settings, today);

            ArgumentCaptor<RecurringExecutionSkippedEvent> captor = ArgumentCaptor.forClass(RecurringExecutionSkippedEvent.class);
            verify(kafkaTemplate).send(eq("activity.recurring.skipped"), captor.capture());

            RecurringExecutionSkippedEvent event = captor.getValue();
            assertThat(event.skippedCount()).isEqualTo(2);
            assertThat(event.lastScheduledDate()).isEqualTo(today);
        }

        @Test
        void shouldSetSkippedNotificationSentTrueAfterSendingNotification() {
            RecurringSettings settings = createValidSettings(today);
            when(recurringExecutionService.execute(eq(settings), any())).thenReturn(RecurringExecutionResult.SKIPPED);

            recurringTransactionProcess.processSingle(settings, today);

            assertThat(settings.isSkippedNotificationSent()).isTrue();
        }

        @Test
        void shouldNotSendNotificationAgainWhenAlreadySent() {
            RecurringSettings settings = createValidSettings(today);
            settings.setSkippedNotificationSent(true);
            when(recurringExecutionService.execute(eq(settings), any())).thenReturn(RecurringExecutionResult.SKIPPED);

            recurringTransactionProcess.processSingle(settings, today);

            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void shouldSaveSettingsRegardlessOfSkippedNotification() {
            RecurringSettings settings = createValidSettings(today);
            when(recurringExecutionService.execute(eq(settings), any())).thenReturn(RecurringExecutionResult.SKIPPED);

            recurringTransactionProcess.processSingle(settings, today);

            verify(recurringSettingsRepository).save(settings);
        }
    }
}