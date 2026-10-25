package com.finovara.financeservice.settings.finances.recurring.processor;

import com.finovara.contracts.mainaccount.transaction.event.transaction.RecurringExecutionSkippedEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.RecurringType;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.financeservice.settings.finances.recurring.service.execution.RecurringExecutionContext;
import com.finovara.financeservice.settings.finances.recurring.service.execution.RecurringExecutionResult;
import com.finovara.financeservice.settings.finances.recurring.service.execution.RecurringExecutionService;
import com.finovara.financeservice.wallet.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionProcessTest {

    private static final Long SETTINGS_ID = 10L;
    private static final Long USER_ID = 1L;
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(150);
    private static final PeriodType PERIOD_TYPE = PeriodType.values()[0];
    private static final LocalDate DATE_1 = LocalDate.of(2025, 1, 1);
    private static final LocalDate DATE_2 = PERIOD_TYPE.addPeriod(DATE_1);
    private static final LocalDate DATE_3 = PERIOD_TYPE.addPeriod(DATE_2);
    private static final LocalDate DATE_4 = PERIOD_TYPE.addPeriod(DATE_3);
    private static final String OUTBOX_AGGREGATE = "RecurringSettings";
    private static final String SKIPPED_TOPIC = "notification.recurring-transaction-skipped";

    @Mock
    private RecurringSettingsRepository recurringSettingsRepository;

    @Mock
    private RecurringExecutionService recurringExecutionService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private RecurringSettings settings;

    @Mock
    private Wallet wallet;

    @InjectMocks
    private RecurringTransactionProcess process;

    @Nested
    class ProcessSingleWhenSettingsNotFound {

        @Test
        void shouldDoNothingWhenSettingsAreNotFound() {
            when(recurringSettingsRepository.findDueByIdForUpdateSkipLocked(SETTINGS_ID, DATE_1)).thenReturn(Optional.empty());

            process.processSingle(SETTINGS_ID, DATE_1);

            verifyNoInteractions(recurringExecutionService, outboxService);
        }
    }

    @Nested
    class ProcessSingleWhenSettingsNotProcessable {

        @BeforeEach
        void setUp() {
            when(recurringSettingsRepository.findDueByIdForUpdateSkipLocked(SETTINGS_ID, DATE_1)).thenReturn(Optional.of(settings));
        }

        @Test
        void shouldDoNothingWhenSettingsAreDisabled() {
            when(settings.isEnable()).thenReturn(false);

            process.processSingle(SETTINGS_ID, DATE_1);

            verifyNoInteractions(recurringExecutionService, outboxService);
            verify(settings, never()).setNextExecutionDate(any(LocalDate.class));
        }

        @Test
        void shouldDoNothingWhenTypeIsNull() {
            when(settings.isEnable()).thenReturn(true);
            when(settings.getType()).thenReturn(null);

            process.processSingle(SETTINGS_ID, DATE_1);

            verifyNoInteractions(recurringExecutionService, outboxService);
            verify(settings, never()).setNextExecutionDate(any(LocalDate.class));
        }

        @Test
        void shouldDoNothingWhenPeriodTypeIsNull() {
            when(settings.isEnable()).thenReturn(true);
            when(settings.getType()).thenReturn(RecurringType.EXPENSE);
            when(settings.getPeriodType()).thenReturn(null);

            process.processSingle(SETTINGS_ID, DATE_1);

            verifyNoInteractions(recurringExecutionService, outboxService);
            verify(settings, never()).setNextExecutionDate(any(LocalDate.class));
        }

        @Test
        void shouldDoNothingWhenAmountIsNull() {
            when(settings.isEnable()).thenReturn(true);
            when(settings.getType()).thenReturn(RecurringType.EXPENSE);
            when(settings.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(settings.getAmount()).thenReturn(null);

            process.processSingle(SETTINGS_ID, DATE_1);

            verifyNoInteractions(recurringExecutionService, outboxService);
            verify(settings, never()).setNextExecutionDate(any(LocalDate.class));
        }

        @Test
        void shouldDoNothingWhenUserIdIsNull() {
            when(settings.isEnable()).thenReturn(true);
            when(settings.getType()).thenReturn(RecurringType.EXPENSE);
            when(settings.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(settings.getAmount()).thenReturn(AMOUNT);
            when(settings.getUserId()).thenReturn(null);

            process.processSingle(SETTINGS_ID, DATE_1);

            verifyNoInteractions(recurringExecutionService, outboxService);
            verify(settings, never()).setNextExecutionDate(any(LocalDate.class));
        }

        @Test
        void shouldDoNothingWhenNextExecutionDateIsNull() {
            when(settings.isEnable()).thenReturn(true);
            when(settings.getType()).thenReturn(RecurringType.EXPENSE);
            when(settings.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(settings.getAmount()).thenReturn(AMOUNT);
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getNextExecutionDate()).thenReturn(null);

            process.processSingle(SETTINGS_ID, DATE_1);

            verifyNoInteractions(recurringExecutionService, outboxService);
            verify(settings, never()).setNextExecutionDate(any(LocalDate.class));
        }
    }

    @Nested
    class ProcessSingleWhenSettingsProcessable {

        private RecurringExecutionContext context;

        @BeforeEach
        void setUp() {
            context = new RecurringExecutionContext(wallet, null, List.of(), false);
            when(settings.getType()).thenReturn(RecurringType.EXPENSE);
            when(settings.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(settings.getAmount()).thenReturn(AMOUNT);
            when(settings.getUserId()).thenReturn(USER_ID);
            when(recurringExecutionService.prepareContext(settings)).thenReturn(context);
        }

        @Test
        void shouldExecuteSingleDueDateAndScheduleNextDateWhenSettingsAreDue() {
            when(recurringSettingsRepository.findDueByIdForUpdateSkipLocked(SETTINGS_ID, DATE_1)).thenReturn(Optional.of(settings));
            when(settings.isEnable()).thenReturn(true);
            when(settings.getNextExecutionDate()).thenReturn(DATE_1);
            when(settings.getEndDate()).thenReturn(null);
            when(recurringExecutionService.execute(settings, DATE_1, context)).thenReturn(RecurringExecutionResult.EXECUTED);

            process.processSingle(SETTINGS_ID, DATE_1);

            verify(recurringExecutionService).execute(settings, DATE_1, context);
            verify(settings).setNextExecutionDate(DATE_2);
            verify(settings).setSkippedNotificationSent(false);
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldPrepareContextOnceWhenSettingsAreDue() {
            when(recurringSettingsRepository.findDueByIdForUpdateSkipLocked(SETTINGS_ID, DATE_1)).thenReturn(Optional.of(settings));
            when(settings.isEnable()).thenReturn(true);
            when(settings.getNextExecutionDate()).thenReturn(DATE_1);
            when(settings.getEndDate()).thenReturn(null);
            when(recurringExecutionService.execute(settings, DATE_1, context)).thenReturn(RecurringExecutionResult.EXECUTED);

            process.processSingle(SETTINGS_ID, DATE_1);

            verify(recurringExecutionService, times(1)).prepareContext(settings);
        }

        @Test
        void shouldExecuteAllOverdueDatesWhenMultipleDatesAreDue() {
            when(recurringSettingsRepository.findDueByIdForUpdateSkipLocked(SETTINGS_ID, DATE_3)).thenReturn(Optional.of(settings));
            when(settings.isEnable()).thenReturn(true);
            when(settings.getNextExecutionDate()).thenReturn(DATE_1);
            when(settings.getEndDate()).thenReturn(null);
            when(recurringExecutionService.execute(eq(settings), any(LocalDate.class), eq(context))).thenReturn(RecurringExecutionResult.EXECUTED);

            process.processSingle(SETTINGS_ID, DATE_3);

            verify(recurringExecutionService).execute(settings, DATE_1, context);
            verify(recurringExecutionService).execute(settings, DATE_2, context);
            verify(recurringExecutionService).execute(settings, DATE_3, context);
            verify(recurringExecutionService, never()).execute(settings, DATE_4, context);
            verify(settings).setNextExecutionDate(DATE_4);
        }

        @Test
        void shouldNotExecuteAndKeepNextDateWhenNextExecutionDateIsAfterToday() {
            when(recurringSettingsRepository.findDueByIdForUpdateSkipLocked(SETTINGS_ID, DATE_1)).thenReturn(Optional.of(settings));
            when(settings.isEnable()).thenReturn(true);
            when(settings.getNextExecutionDate()).thenReturn(DATE_3);
            when(settings.getEndDate()).thenReturn(null);

            process.processSingle(SETTINGS_ID, DATE_1);

            verify(recurringExecutionService, never()).execute(any(RecurringSettings.class), any(LocalDate.class), any(RecurringExecutionContext.class));
            verify(settings).setNextExecutionDate(DATE_3);
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldStopAtEndDateAndClearNextDateWhenEndDateIsReached() {
            when(recurringSettingsRepository.findDueByIdForUpdateSkipLocked(SETTINGS_ID, DATE_4)).thenReturn(Optional.of(settings));
            when(settings.isEnable()).thenReturn(true);
            when(settings.getNextExecutionDate()).thenReturn(DATE_1);
            when(settings.getEndDate()).thenReturn(DATE_2);
            when(recurringExecutionService.execute(eq(settings), any(LocalDate.class), eq(context))).thenReturn(RecurringExecutionResult.EXECUTED);

            process.processSingle(SETTINGS_ID, DATE_4);

            verify(recurringExecutionService).execute(settings, DATE_1, context);
            verify(recurringExecutionService).execute(settings, DATE_2, context);
            verify(recurringExecutionService, never()).execute(settings, DATE_3, context);
            verify(settings).setNextExecutionDate(null);
        }

        @Test
        void shouldExecuteOnlyOnceAndClearNextDateWhenEndDateEqualsNextExecutionDate() {
            when(recurringSettingsRepository.findDueByIdForUpdateSkipLocked(SETTINGS_ID, DATE_1)).thenReturn(Optional.of(settings));
            when(settings.isEnable()).thenReturn(true);
            when(settings.getNextExecutionDate()).thenReturn(DATE_1);
            when(settings.getEndDate()).thenReturn(DATE_1);
            when(recurringExecutionService.execute(settings, DATE_1, context)).thenReturn(RecurringExecutionResult.EXECUTED);

            process.processSingle(SETTINGS_ID, DATE_1);

            verify(recurringExecutionService, times(1)).execute(settings, DATE_1, context);
            verify(settings).setNextExecutionDate(null);
        }

        @Test
        void shouldKeepNextDateWhenEndDateIsNotReachedYet() {
            when(recurringSettingsRepository.findDueByIdForUpdateSkipLocked(SETTINGS_ID, DATE_2)).thenReturn(Optional.of(settings));
            when(settings.isEnable()).thenReturn(true);
            when(settings.getNextExecutionDate()).thenReturn(DATE_1);
            when(settings.getEndDate()).thenReturn(DATE_4);
            when(recurringExecutionService.execute(eq(settings), any(LocalDate.class), eq(context))).thenReturn(RecurringExecutionResult.EXECUTED);

            process.processSingle(SETTINGS_ID, DATE_2);

            verify(settings).setNextExecutionDate(DATE_3);
        }

        @Test
        void shouldLimitExecutionsToMaximumIterationsWhenManyDatesAreDue() {
            LocalDate farFuture = DATE_1.plusYears(1000);
            when(recurringSettingsRepository.findDueByIdForUpdateSkipLocked(SETTINGS_ID, farFuture)).thenReturn(Optional.of(settings));
            when(settings.isEnable()).thenReturn(true);
            when(settings.getNextExecutionDate()).thenReturn(DATE_1);
            when(settings.getEndDate()).thenReturn(null);
            when(recurringExecutionService.execute(eq(settings), any(LocalDate.class), eq(context))).thenReturn(RecurringExecutionResult.EXECUTED);

            process.processSingle(SETTINGS_ID, farFuture);

            verify(recurringExecutionService, times(100)).execute(eq(settings), any(LocalDate.class), eq(context));
            verify(settings, times(1)).setNextExecutionDate(any(LocalDate.class));
        }

        @Test
        void shouldSaveSkippedEventAndMarkNotificationSentWhenExecutionIsSkipped() {
            when(recurringSettingsRepository.findDueByIdForUpdateSkipLocked(SETTINGS_ID, DATE_1)).thenReturn(Optional.of(settings));
            when(settings.isEnable()).thenReturn(true);
            when(settings.getId()).thenReturn(SETTINGS_ID);
            when(settings.getNextExecutionDate()).thenReturn(DATE_1);
            when(settings.getEndDate()).thenReturn(null);
            when(settings.isSkippedNotificationSent()).thenReturn(false);
            when(recurringExecutionService.execute(settings, DATE_1, context)).thenReturn(RecurringExecutionResult.SKIPPED);

            process.processSingle(SETTINGS_ID, DATE_1);

            verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq("10"), eq(SKIPPED_TOPIC), any(RecurringExecutionSkippedEvent.class));
            verify(settings).setSkippedNotificationSent(true);
            verify(settings, never()).setSkippedNotificationSent(false);
            verify(settings).setNextExecutionDate(DATE_2);
        }

        @Test
        void shouldNotSaveSkippedEventWhenNotificationWasAlreadySent() {
            when(recurringSettingsRepository.findDueByIdForUpdateSkipLocked(SETTINGS_ID, DATE_1)).thenReturn(Optional.of(settings));
            when(settings.isEnable()).thenReturn(true);
            when(settings.getNextExecutionDate()).thenReturn(DATE_1);
            when(settings.getEndDate()).thenReturn(null);
            when(settings.isSkippedNotificationSent()).thenReturn(true);
            when(recurringExecutionService.execute(settings, DATE_1, context)).thenReturn(RecurringExecutionResult.SKIPPED);

            process.processSingle(SETTINGS_ID, DATE_1);

            verifyNoInteractions(outboxService);
            verify(settings, never()).setSkippedNotificationSent(true);
            verify(settings).setNextExecutionDate(DATE_2);
        }

        @Test
        void shouldSaveSingleSkippedEventWhenMultipleExecutionsAreSkipped() {
            when(recurringSettingsRepository.findDueByIdForUpdateSkipLocked(SETTINGS_ID, DATE_3)).thenReturn(Optional.of(settings));
            when(settings.isEnable()).thenReturn(true);
            when(settings.getId()).thenReturn(SETTINGS_ID);
            when(settings.getNextExecutionDate()).thenReturn(DATE_1);
            when(settings.getEndDate()).thenReturn(null);
            when(settings.isSkippedNotificationSent()).thenReturn(false);
            when(recurringExecutionService.execute(eq(settings), any(LocalDate.class), eq(context))).thenReturn(RecurringExecutionResult.SKIPPED);

            process.processSingle(SETTINGS_ID, DATE_3);

            verify(recurringExecutionService, times(3)).execute(eq(settings), any(LocalDate.class), eq(context));
            verify(outboxService, times(1)).save(eq(OUTBOX_AGGREGATE), eq("10"), eq(SKIPPED_TOPIC), any(RecurringExecutionSkippedEvent.class));
            verify(settings).setNextExecutionDate(DATE_4);
        }

        @Test
        void shouldResetNotificationFlagAndStillNotifyWhenSkippedAndExecutedDatesAreMixed() {
            when(recurringSettingsRepository.findDueByIdForUpdateSkipLocked(SETTINGS_ID, DATE_2)).thenReturn(Optional.of(settings));
            when(settings.isEnable()).thenReturn(true);
            when(settings.getId()).thenReturn(SETTINGS_ID);
            when(settings.getNextExecutionDate()).thenReturn(DATE_1);
            when(settings.getEndDate()).thenReturn(null);
            when(settings.isSkippedNotificationSent()).thenReturn(false);
            when(recurringExecutionService.execute(settings, DATE_1, context)).thenReturn(RecurringExecutionResult.SKIPPED);
            when(recurringExecutionService.execute(settings, DATE_2, context)).thenReturn(RecurringExecutionResult.EXECUTED);

            process.processSingle(SETTINGS_ID, DATE_2);

            verify(settings).setSkippedNotificationSent(false);
            verify(outboxService, times(1)).save(eq(OUTBOX_AGGREGATE), eq("10"), eq(SKIPPED_TOPIC), any(RecurringExecutionSkippedEvent.class));
            verify(settings).setSkippedNotificationSent(true);
        }

        @Test
        void shouldStopAndClearNextDateWhenSettingsGetDisabledDuringExecution() {
            when(recurringSettingsRepository.findDueByIdForUpdateSkipLocked(SETTINGS_ID, DATE_3)).thenReturn(Optional.of(settings));
            when(settings.isEnable()).thenReturn(true, true, false);
            when(settings.getNextExecutionDate()).thenReturn(DATE_1);
            when(settings.getEndDate()).thenReturn(null);
            when(recurringExecutionService.execute(settings, DATE_1, context)).thenReturn(RecurringExecutionResult.EXECUTED);

            process.processSingle(SETTINGS_ID, DATE_3);

            verify(recurringExecutionService, times(1)).execute(eq(settings), any(LocalDate.class), eq(context));
            verify(settings, never()).setSkippedNotificationSent(false);
            verify(settings).setNextExecutionDate(null);
        }

        @Test
        void shouldNotifySkippedAndClearNextDateWhenSettingsGetDisabledAfterSkippedExecution() {
            when(recurringSettingsRepository.findDueByIdForUpdateSkipLocked(SETTINGS_ID, DATE_3)).thenReturn(Optional.of(settings));
            when(settings.isEnable()).thenReturn(true, true, false);
            when(settings.getId()).thenReturn(SETTINGS_ID);
            when(settings.getNextExecutionDate()).thenReturn(DATE_1);
            when(settings.getEndDate()).thenReturn(null);
            when(settings.isSkippedNotificationSent()).thenReturn(false);
            when(recurringExecutionService.execute(settings, DATE_1, context)).thenReturn(RecurringExecutionResult.SKIPPED);

            process.processSingle(SETTINGS_ID, DATE_3);

            verify(recurringExecutionService, times(1)).execute(eq(settings), any(LocalDate.class), eq(context));
            verify(outboxService).save(eq(OUTBOX_AGGREGATE), eq("10"), eq(SKIPPED_TOPIC), any(RecurringExecutionSkippedEvent.class));
            verify(settings).setNextExecutionDate(null);
        }

        @Test
        void shouldThrowExceptionWhenExecutionFailsUnexpectedly() {
            when(recurringSettingsRepository.findDueByIdForUpdateSkipLocked(SETTINGS_ID, DATE_1)).thenReturn(Optional.of(settings));
            when(settings.isEnable()).thenReturn(true);
            when(settings.getNextExecutionDate()).thenReturn(DATE_1);
            when(settings.getEndDate()).thenReturn(null);
            when(recurringExecutionService.execute(settings, DATE_1, context)).thenThrow(new IllegalStateException("unexpected"));

            assertThrows(IllegalStateException.class, () -> process.processSingle(SETTINGS_ID, DATE_1));

            verify(settings, never()).setNextExecutionDate(any(LocalDate.class));
            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    class ProcessSingleWhenPreparingContextFails {

        @Test
        void shouldThrowExceptionWhenPreparingContextFails() {
            when(recurringSettingsRepository.findDueByIdForUpdateSkipLocked(SETTINGS_ID, DATE_1)).thenReturn(Optional.of(settings));
            when(settings.isEnable()).thenReturn(true);
            when(settings.getType()).thenReturn(RecurringType.EXPENSE);
            when(settings.getPeriodType()).thenReturn(PERIOD_TYPE);
            when(settings.getAmount()).thenReturn(AMOUNT);
            when(settings.getUserId()).thenReturn(USER_ID);
            when(settings.getNextExecutionDate()).thenReturn(DATE_1);
            when(recurringExecutionService.prepareContext(settings)).thenThrow(new IllegalStateException("wallet"));

            assertThrows(IllegalStateException.class, () -> process.processSingle(SETTINGS_ID, DATE_1));

            verify(recurringExecutionService, never()).execute(any(RecurringSettings.class), any(LocalDate.class), any(RecurringExecutionContext.class));
            verifyNoInteractions(outboxService);
        }
    }
}