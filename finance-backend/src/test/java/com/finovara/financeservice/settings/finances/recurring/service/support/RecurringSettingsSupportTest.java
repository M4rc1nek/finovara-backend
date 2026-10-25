package com.finovara.financeservice.settings.finances.recurring.service.support;

import com.finovara.contracts.mainaccount.activity.event.settings.SettingsActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.SettingType;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.RecurringType;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringCommonFields;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringSettingsSupportTest {

    private static final Long USER_ID = 1L;
    private static final Long SETTINGS_ID = 5L;
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(100);
    private static final PeriodType PERIOD_TYPE = PeriodType.values()[0];
    private static final LocalDate START_DATE = LocalDate.of(2025, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2025, 12, 31);

    @Mock
    private RecurringSettingsRepository recurringSettingsRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private RecurringSettings settings;

    @InjectMocks
    private RecurringSettingsSupport support;

    @Nested
    class GetSettings {

        @Test
        void shouldReturnSettingsWhenSettingsExist() {
            when(recurringSettingsRepository.findByUserIdAndType(USER_ID, RecurringType.EXPENSE)).thenReturn(Optional.of(settings));

            RecurringSettings result = support.getSettings(USER_ID, RecurringType.EXPENSE);

            assertSame(settings, result);
        }

        @Test
        void shouldThrowExceptionWhenSettingsDoNotExist() {
            when(recurringSettingsRepository.findByUserIdAndType(USER_ID, RecurringType.REVENUE)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> support.getSettings(USER_ID, RecurringType.REVENUE));
        }
    }

    @Nested
    class GetSettingsForUpdate {

        @Test
        void shouldReturnSettingsWhenSettingsExist() {
            when(recurringSettingsRepository.findByUserIdAndTypeForUpdate(USER_ID, RecurringType.SAVINGS)).thenReturn(Optional.of(settings));

            RecurringSettings result = support.getSettingsForUpdate(USER_ID, RecurringType.SAVINGS);

            assertSame(settings, result);
        }

        @Test
        void shouldThrowExceptionWhenSettingsDoNotExist() {
            when(recurringSettingsRepository.findByUserIdAndTypeForUpdate(USER_ID, RecurringType.SAVINGS)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> support.getSettingsForUpdate(USER_ID, RecurringType.SAVINGS));
        }
    }

    @Nested
    class ApplyCommonFields {

        @Test
        void shouldApplyAllFieldsAndScheduleFirstExecutionWhenEnabled() {
            RecurringCommonFields fields = new RecurringCommonFields(true, AMOUNT, PERIOD_TYPE, START_DATE, END_DATE);
            when(settings.getId()).thenReturn(SETTINGS_ID);

            support.applyCommonFields(USER_ID, settings, fields, SettingType.EXPENSE_RECURRING);

            verify(settings).setEnable(true);
            verify(settings).setAmount(AMOUNT);
            verify(settings).setPeriodType(PERIOD_TYPE);
            verify(settings).setStartDate(START_DATE);
            verify(settings).setEndDate(END_DATE);
            verify(settings).setNextExecutionDate(START_DATE);
        }

        @Test
        void shouldSaveOutboxEventWhenEnabled() {
            RecurringCommonFields fields = new RecurringCommonFields(true, AMOUNT, PERIOD_TYPE, START_DATE, END_DATE);
            when(settings.getId()).thenReturn(SETTINGS_ID);

            support.applyCommonFields(USER_ID, settings, fields, SettingType.REVENUE_RECURRING);

            verify(outboxService).save(eq("RecurringSettings"), eq("5"), eq("settings.changed"), any(SettingsActivityEvent.class));
        }

        @Test
        void shouldClearNextExecutionAndKeepDatesWhenDisabled() {
            RecurringCommonFields fields = new RecurringCommonFields(false, AMOUNT, PERIOD_TYPE, START_DATE, END_DATE);
            when(settings.getId()).thenReturn(SETTINGS_ID);

            support.applyCommonFields(USER_ID, settings, fields, SettingType.SAVINGS_RECURRING);

            verify(settings).setEnable(false);
            verify(settings).setAmount(AMOUNT);
            verify(settings).setPeriodType(PERIOD_TYPE);
            verify(settings).setNextExecutionDate(null);
            verify(settings, never()).setStartDate(any(LocalDate.class));
            verify(settings, never()).setEndDate(any(LocalDate.class));
        }

        @Test
        void shouldSaveOutboxEventWhenDisabled() {
            RecurringCommonFields fields = new RecurringCommonFields(false, AMOUNT, PERIOD_TYPE, START_DATE, END_DATE);
            when(settings.getId()).thenReturn(SETTINGS_ID);

            support.applyCommonFields(USER_ID, settings, fields, SettingType.EXPENSE_RECURRING);

            verify(outboxService).save(eq("RecurringSettings"), eq("5"), eq("settings.changed"), any(SettingsActivityEvent.class));
        }

        @Test
        void shouldTreatNullEnableAsDisabledWhenEnableIsNull() {
            RecurringCommonFields fields = new RecurringCommonFields(null, AMOUNT, PERIOD_TYPE, START_DATE, END_DATE);
            when(settings.getId()).thenReturn(SETTINGS_ID);

            support.applyCommonFields(USER_ID, settings, fields, SettingType.EXPENSE_RECURRING);

            verify(settings).setEnable(false);
            verify(settings).setNextExecutionDate(null);
            verify(settings, never()).setStartDate(any(LocalDate.class));
        }

        @Test
        void shouldThrowExceptionWhenSettingsIdIsNull() {
            RecurringCommonFields fields = new RecurringCommonFields(true, AMOUNT, PERIOD_TYPE, START_DATE, END_DATE);
            when(settings.getId()).thenReturn(null);

            assertThrows(NullPointerException.class, () -> support.applyCommonFields(USER_ID, settings, fields, SettingType.EXPENSE_RECURRING));

            verifyNoInteractions(outboxService);
        }
    }
}