package com.finovara.financeservice.settings.finances.recurring.processor;

import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.financeservice.settings.finances.recurring.service.execution.RecurringExecutionService;
import com.finovara.contracts.model.PeriodType;
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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringProcessorTest {

    @Mock
    private RecurringSettingsRepository recurringSettingsRepository;

    @Mock
    private RecurringExecutionService executionService;

    @InjectMocks
    private RecurringProcessor recurringProcessor;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
    }

    private RecurringSettings createValidSettings(LocalDate nextExecutionDate) {
        RecurringSettings settings = new RecurringSettings();

        settings.setEnable(true);
        settings.setNextExecutionDate(nextExecutionDate);
        settings.setAmount(BigDecimal.valueOf(100));
        settings.setUserId(1L);

        settings.setPeriodType(PeriodType.DAILY);

        return settings;
    }

    @Nested
    class GenerateRecurringTransaction {
        @Test
        void shouldSkipInvalidSettings() {
            RecurringSettings settings = new RecurringSettings();
            settings.setEnable(false);

            when(recurringSettingsRepository.findDueRecurring(today)).thenReturn(List.of(settings));

            recurringProcessor.generateRecurringTransaction();

            verifyNoInteractions(executionService);
            verify(recurringSettingsRepository, never()).save(any());
        }

        @Test
        void shouldProcessValidSettings() {
            RecurringSettings settings = createValidSettings(today);

            when(recurringSettingsRepository.findDueRecurring(today)).thenReturn(List.of(settings));

            recurringProcessor.generateRecurringTransaction();

            verify(executionService).execute(settings, today);
            verify(recurringSettingsRepository).save(settings);
        }
    }

    @Nested
    class ProcessSingle {
        @Test
        void shouldExecuteOnceWhenDateIsToday() {
            RecurringSettings settings = createValidSettings(today);

            when(recurringSettingsRepository.findDueRecurring(today)).thenReturn(List.of(settings));

            recurringProcessor.generateRecurringTransaction();

            verify(executionService, times(1)).execute(settings, today);
        }

        @Test
        void shouldExecuteMultipleTimesForPastDates() {
            RecurringSettings settings = createValidSettings(today.minusDays(3));

            when(recurringSettingsRepository.findDueRecurring(today)).thenReturn(List.of(settings));

            recurringProcessor.generateRecurringTransaction();

            verify(executionService, times(4)).execute(eq(settings), any());
        }

        @Test
        void shouldStopWhenDisabledDuringExecution() {
            RecurringSettings settings = createValidSettings(today.minusDays(2));

            doAnswer(invocation -> {
                settings.setEnable(false);
                return null;
            }).when(executionService).execute(eq(settings), any());

            when(recurringSettingsRepository.findDueRecurring(today)).thenReturn(List.of(settings));

            recurringProcessor.generateRecurringTransaction();

            verify(executionService, times(1)).execute(eq(settings), any());
        }

        @Test
        void shouldRespectMaxIterationsLimit() {
            RecurringSettings settings = createValidSettings(today.minusDays(200));

            when(recurringSettingsRepository.findDueRecurring(today)).thenReturn(List.of(settings));

            recurringProcessor.generateRecurringTransaction();

            verify(executionService, atMost(100)).execute(eq(settings), any());
        }

        @Test
        void shouldUpdateNextExecutionDate() {
            RecurringSettings settings = createValidSettings(today.minusDays(1));

            when(recurringSettingsRepository.findDueRecurring(today)).thenReturn(List.of(settings));

            recurringProcessor.generateRecurringTransaction();

            verify(recurringSettingsRepository).save(settings);
        }

        @Test
        void shouldSetNextExecutionDateToNullWhenDisabled() {
            RecurringSettings settings = createValidSettings(today);

            doAnswer(invocation -> {
                settings.setEnable(false);
                return null;
            }).when(executionService).execute(eq(settings), any());

            when(recurringSettingsRepository.findDueRecurring(today)).thenReturn(List.of(settings));

            recurringProcessor.generateRecurringTransaction();

            assertNull(settings.getNextExecutionDate());
        }

        @Test
        void shouldSkipWhenNextExecutionDateIsNull() {
            RecurringSettings settings = createValidSettings(today);
            settings.setNextExecutionDate(null);

            when(recurringSettingsRepository.findDueRecurring(today)).thenReturn(List.of(settings));

            recurringProcessor.generateRecurringTransaction();

            verifyNoInteractions(executionService);
        }

        @Test
        void shouldSkipWhenEndDateIsNull() {
            RecurringSettings settings = createValidSettings(today);
            settings.setEndDate(null);

            recurringProcessor.generateRecurringTransaction();

            verifyNoInteractions(executionService);
        }
    }
}