package com.finovara.financeservice.settings.finances.recurring.processor;

import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
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

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringProcessorTest {

    @Mock
    private RecurringSettingsRepository recurringSettingsRepository;

    @Mock
    private RecurringTransactionProcess recurringTransactionProcess;

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

        return settings;
    }

    @Nested
    class GenerateRecurringTransactionTests {

        @Test
        void shouldSkipDisabledSettings() {
            RecurringSettings settings = createValidSettings(today);
            settings.setEnable(false);

            when(recurringSettingsRepository.findDueRecurring(today)).thenReturn(List.of(settings));

            recurringProcessor.generateRecurringTransaction();

            verifyNoInteractions(recurringTransactionProcess);
        }

        @Test
        void shouldSkipSettingsWhenNextExecutionDateIsNull() {
            RecurringSettings settings = createValidSettings(null);

            when(recurringSettingsRepository.findDueRecurring(today)).thenReturn(List.of(settings));

            recurringProcessor.generateRecurringTransaction();

            verifyNoInteractions(recurringTransactionProcess);
        }

        @Test
        void shouldSkipSettingsWhenPeriodTypeIsNull() {
            RecurringSettings settings = createValidSettings(today);
            settings.setPeriodType(null);

            when(recurringSettingsRepository.findDueRecurring(today)).thenReturn(List.of(settings));

            recurringProcessor.generateRecurringTransaction();

            verifyNoInteractions(recurringTransactionProcess);
        }

        @Test
        void shouldSkipSettingsWhenAmountIsNull() {
            RecurringSettings settings = createValidSettings(today);
            settings.setAmount(null);

            when(recurringSettingsRepository.findDueRecurring(today)).thenReturn(List.of(settings));

            recurringProcessor.generateRecurringTransaction();

            verifyNoInteractions(recurringTransactionProcess);
        }

        @Test
        void shouldSkipSettingsWhenUserIdIsNull() {
            RecurringSettings settings = createValidSettings(today);
            settings.setUserId(null);

            when(recurringSettingsRepository.findDueRecurring(today)).thenReturn(List.of(settings));

            recurringProcessor.generateRecurringTransaction();

            verifyNoInteractions(recurringTransactionProcess);
        }

        @Test
        void shouldDoNothingWhenNoDueSettingsExist() {
            when(recurringSettingsRepository.findDueRecurring(today)).thenReturn(List.of());

            recurringProcessor.generateRecurringTransaction();

            verifyNoInteractions(recurringTransactionProcess);
        }

    }
}