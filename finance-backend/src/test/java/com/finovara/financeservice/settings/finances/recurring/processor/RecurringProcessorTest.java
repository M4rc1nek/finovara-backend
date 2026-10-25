package com.finovara.financeservice.settings.finances.recurring.processor;

import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringProcessorTest {

    private static final int BATCH_SIZE = 3;

    @Mock
    private RecurringSettingsRepository recurringSettingsRepository;

    @Mock
    private RecurringTransactionProcess recurringTransactionProcess;

    @InjectMocks
    private RecurringProcessor processor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(processor, "batchSize", BATCH_SIZE);
    }

    @Nested
    class GenerateRecurringTransaction {

        @Test
        void shouldNotProcessAnythingWhenNoDueSettingsExist() {
            when(recurringSettingsRepository.findDueSettingsIds(any(LocalDate.class), eq(0L), eq(PageRequest.of(0, BATCH_SIZE)))).thenReturn(List.of());

            processor.generateRecurringTransaction();

            verify(recurringSettingsRepository, times(1)).findDueSettingsIds(any(LocalDate.class), eq(0L), eq(PageRequest.of(0, BATCH_SIZE)));
            verifyNoInteractions(recurringTransactionProcess);
        }

        @Test
        void shouldProcessEachSettingsAndQueryOnceWhenDueSettingsAreFewerThanBatchSize() {
            when(recurringSettingsRepository.findDueSettingsIds(any(LocalDate.class), eq(0L), eq(PageRequest.of(0, BATCH_SIZE)))).thenReturn(List.of(1L, 2L));

            processor.generateRecurringTransaction();

            verify(recurringTransactionProcess).processSingle(eq(1L), any(LocalDate.class));
            verify(recurringTransactionProcess).processSingle(eq(2L), any(LocalDate.class));
            verify(recurringSettingsRepository, times(1)).findDueSettingsIds(any(LocalDate.class), any(Long.class), any(PageRequest.class));
        }

        @Test
        void shouldQueryAgainWithLastProcessedIdWhenDueSettingsEqualBatchSize() {
            when(recurringSettingsRepository.findDueSettingsIds(any(LocalDate.class), eq(0L), eq(PageRequest.of(0, BATCH_SIZE)))).thenReturn(List.of(1L, 2L, 3L));
            when(recurringSettingsRepository.findDueSettingsIds(any(LocalDate.class), eq(3L), eq(PageRequest.of(0, BATCH_SIZE)))).thenReturn(List.of());

            processor.generateRecurringTransaction();

            verify(recurringSettingsRepository).findDueSettingsIds(any(LocalDate.class), eq(3L), eq(PageRequest.of(0, BATCH_SIZE)));
            verify(recurringTransactionProcess, times(3)).processSingle(any(Long.class), any(LocalDate.class));
        }

        @Test
        void shouldProcessAllPagesWhenMultipleBatchesExist() {
            when(recurringSettingsRepository.findDueSettingsIds(any(LocalDate.class), eq(0L), eq(PageRequest.of(0, BATCH_SIZE)))).thenReturn(List.of(1L, 2L, 3L));
            when(recurringSettingsRepository.findDueSettingsIds(any(LocalDate.class), eq(3L), eq(PageRequest.of(0, BATCH_SIZE)))).thenReturn(List.of(4L, 5L));

            processor.generateRecurringTransaction();

            verify(recurringTransactionProcess, times(5)).processSingle(any(Long.class), any(LocalDate.class));
            verify(recurringSettingsRepository, times(2)).findDueSettingsIds(any(LocalDate.class), any(Long.class), any(PageRequest.class));
            verify(recurringSettingsRepository, never()).findDueSettingsIds(any(LocalDate.class), eq(5L), any(PageRequest.class));
        }

        @Test
        void shouldQueryUsingLastIdOfPreviousBatchWhenBatchSizeIsOne() {
            ReflectionTestUtils.setField(processor, "batchSize", 1);
            when(recurringSettingsRepository.findDueSettingsIds(any(LocalDate.class), eq(0L), eq(PageRequest.of(0, 1)))).thenReturn(List.of(7L));
            when(recurringSettingsRepository.findDueSettingsIds(any(LocalDate.class), eq(7L), eq(PageRequest.of(0, 1)))).thenReturn(List.of());

            processor.generateRecurringTransaction();

            verify(recurringTransactionProcess).processSingle(eq(7L), any(LocalDate.class));
            verify(recurringSettingsRepository, times(2)).findDueSettingsIds(any(LocalDate.class), any(Long.class), any(PageRequest.class));
        }

        @Test
        void shouldContinueProcessingRemainingSettingsWhenOneSettingsFails() {
            when(recurringSettingsRepository.findDueSettingsIds(any(LocalDate.class), eq(0L), eq(PageRequest.of(0, BATCH_SIZE)))).thenReturn(List.of(1L, 2L, 3L));
            when(recurringSettingsRepository.findDueSettingsIds(any(LocalDate.class), eq(3L), eq(PageRequest.of(0, BATCH_SIZE)))).thenReturn(List.of());
            doThrow(new IllegalStateException("failure")).when(recurringTransactionProcess).processSingle(eq(2L), any(LocalDate.class));

            processor.generateRecurringTransaction();

            verify(recurringTransactionProcess).processSingle(eq(1L), any(LocalDate.class));
            verify(recurringTransactionProcess).processSingle(eq(2L), any(LocalDate.class));
            verify(recurringTransactionProcess).processSingle(eq(3L), any(LocalDate.class));
        }

        @Test
        void shouldNotPropagateExceptionWhenProcessingSettingsFails() {
            when(recurringSettingsRepository.findDueSettingsIds(any(LocalDate.class), eq(0L), eq(PageRequest.of(0, BATCH_SIZE)))).thenReturn(List.of(1L));
            doThrow(new IllegalStateException("failure")).when(recurringTransactionProcess).processSingle(eq(1L), any(LocalDate.class));

            assertDoesNotThrow(() -> processor.generateRecurringTransaction());
        }

        @Test
        void shouldContinueToNextBatchWhenAllSettingsInBatchFail() {
            when(recurringSettingsRepository.findDueSettingsIds(any(LocalDate.class), eq(0L), eq(PageRequest.of(0, BATCH_SIZE)))).thenReturn(List.of(1L, 2L, 3L));
            when(recurringSettingsRepository.findDueSettingsIds(any(LocalDate.class), eq(3L), eq(PageRequest.of(0, BATCH_SIZE)))).thenReturn(List.of(4L));
            doThrow(new IllegalStateException("failure")).when(recurringTransactionProcess).processSingle(any(Long.class), any(LocalDate.class));

            processor.generateRecurringTransaction();

            verify(recurringTransactionProcess, times(4)).processSingle(any(Long.class), any(LocalDate.class));
        }
    }
}