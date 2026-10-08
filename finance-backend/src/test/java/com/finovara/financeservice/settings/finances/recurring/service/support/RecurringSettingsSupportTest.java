package com.finovara.financeservice.settings.finances.recurring.service.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finovara.contracts.mainaccount.activity.event.settings.SettingsActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.SettingActivityStatus;
import com.finovara.contracts.mainaccount.activity.model.SettingType;
import com.finovara.contracts.util.model.RecurringType;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringCommonFields;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.repository.RecurringSettingsRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class RecurringSettingsSupportTest {

    private static final Long USER_ID = 1L;
    private static final String TOPIC = "settings.changed";
    private static final BigDecimal AMOUNT = new BigDecimal("150.00");
    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 10);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 10);
    private static final RecurringType RECURRING_TYPE = RecurringType.values()[0];
    private static final SettingType SETTING_TYPE = SettingType.values()[0];

    @Mock
    private RecurringSettingsRepository recurringSettingsRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private RecurringSettings settings;

    @Mock
    private RecurringCommonFields fields;

    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    @InjectMocks
    private RecurringSettingsSupport support;

    @Nested
    class GetSettings {

        @Test
        void shouldReturnSettingsWhenSettingsExist() {
            when(recurringSettingsRepository.findByUserIdAndType(USER_ID, RECURRING_TYPE)).thenReturn(Optional.of(settings));

            RecurringSettings result = support.getSettings(USER_ID, RECURRING_TYPE);

            assertSame(settings, result);
            verify(recurringSettingsRepository).findByUserIdAndType(USER_ID, RECURRING_TYPE);
        }

        @Test
        void shouldNotInteractWithKafkaWhenGettingSettings() {
            when(recurringSettingsRepository.findByUserIdAndType(USER_ID, RECURRING_TYPE)).thenReturn(Optional.of(settings));

            support.getSettings(USER_ID, RECURRING_TYPE);

            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void shouldThrowExceptionWhenSettingsDoNotExist() {
            when(recurringSettingsRepository.findByUserIdAndType(USER_ID, RECURRING_TYPE)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> support.getSettings(USER_ID, RECURRING_TYPE));
        }

        @Test
        void shouldThrowExceptionWhenSettingsDoNotExistForNullUserAndType() {
            when(recurringSettingsRepository.findByUserIdAndType(null, null)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> support.getSettings(null, null));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFails() {
            when(recurringSettingsRepository.findByUserIdAndType(USER_ID, RECURRING_TYPE))
                    .thenThrow(new IllegalStateException("db failed"));

            assertThrows(IllegalStateException.class, () -> support.getSettings(USER_ID, RECURRING_TYPE));
        }
    }

    @Nested
    class ApplyCommonFields {

        private void stubEnabledFields() {
            when(fields.enable()).thenReturn(true);
            when(fields.amount()).thenReturn(AMOUNT);
            when(fields.startDate()).thenReturn(START_DATE);
            when(fields.endDate()).thenReturn(END_DATE);
        }

        private void stubDisabledFields() {
            when(fields.enable()).thenReturn(false);
            when(fields.amount()).thenReturn(AMOUNT);
        }

        @Test
        void shouldSetCommonFieldsWhenSettingsAreEnabled() {
            stubEnabledFields();

            support.applyCommonFields(USER_ID, settings, fields, SETTING_TYPE);

            verify(settings).setEnable(true);
            verify(settings).setAmount(AMOUNT);
            verify(settings).setPeriodType(fields.periodType());
        }

        @Test
        void shouldSetDatesWhenSettingsAreEnabled() {
            stubEnabledFields();

            support.applyCommonFields(USER_ID, settings, fields, SETTING_TYPE);

            verify(settings).setStartDate(START_DATE);
            verify(settings).setEndDate(END_DATE);
        }

        @Test
        void shouldSetNextExecutionDateToStartDateWhenSettingsAreEnabled() {
            stubEnabledFields();

            support.applyCommonFields(USER_ID, settings, fields, SETTING_TYPE);

            verify(settings).setNextExecutionDate(START_DATE);
        }

        @Test
        void shouldSendEnabledEventWhenSettingsAreEnabled() {
            stubEnabledFields();

            support.applyCommonFields(USER_ID, settings, fields, SETTING_TYPE);

            verify(kafkaTemplate).send(eq(TOPIC), eventCaptor.capture());
            SettingsActivityEvent event = (SettingsActivityEvent) eventCaptor.getValue();
            assertEquals(USER_ID, event.userId());
            assertEquals(SETTING_TYPE, event.settingType());
            assertEquals(SettingActivityStatus.ENABLED, event.status());
            assertNotNull(event.occurredAt());
        }

        @Test
        void shouldSetStartAndEndDatesToNullWhenSettingsAreEnabledWithoutDates() {
            when(fields.enable()).thenReturn(true);
            when(fields.amount()).thenReturn(AMOUNT);

            support.applyCommonFields(USER_ID, settings, fields, SETTING_TYPE);

            verify(settings).setStartDate(null);
            verify(settings).setEndDate(null);
            verify(settings).setNextExecutionDate(null);
        }

        @Test
        void shouldSetCommonFieldsWhenSettingsAreDisabled() {
            stubDisabledFields();

            support.applyCommonFields(USER_ID, settings, fields, SETTING_TYPE);

            verify(settings).setEnable(false);
            verify(settings).setAmount(AMOUNT);
            verify(settings).setPeriodType(fields.periodType());
        }

        @Test
        void shouldClearNextExecutionDateWhenSettingsAreDisabled() {
            stubDisabledFields();

            support.applyCommonFields(USER_ID, settings, fields, SETTING_TYPE);

            verify(settings).setNextExecutionDate(null);
        }

        @Test
        void shouldNotChangeDatesWhenSettingsAreDisabled() {
            stubDisabledFields();

            support.applyCommonFields(USER_ID, settings, fields, SETTING_TYPE);

            verify(settings, never()).setStartDate(any());
            verify(settings, never()).setEndDate(any());
        }

        @Test
        void shouldSendDisabledEventWhenSettingsAreDisabled() {
            stubDisabledFields();

            support.applyCommonFields(USER_ID, settings, fields, SETTING_TYPE);

            verify(kafkaTemplate).send(eq(TOPIC), eventCaptor.capture());
            SettingsActivityEvent event = (SettingsActivityEvent) eventCaptor.getValue();
            assertEquals(USER_ID, event.userId());
            assertEquals(SETTING_TYPE, event.settingType());
            assertEquals(SettingActivityStatus.DISABLED, event.status());
            assertNotNull(event.occurredAt());
        }

        @Test
        void shouldTreatSettingsAsDisabledWhenEnableFlagIsNull() {
            when(fields.enable()).thenReturn(null);
            when(fields.amount()).thenReturn(AMOUNT);

            support.applyCommonFields(USER_ID, settings, fields, SETTING_TYPE);

            verify(settings).setEnable(false);
            verify(settings).setNextExecutionDate(null);
            verify(settings, never()).setStartDate(any());
            verify(kafkaTemplate).send(eq(TOPIC), eventCaptor.capture());
            SettingsActivityEvent event = (SettingsActivityEvent) eventCaptor.getValue();
            assertEquals(SettingActivityStatus.DISABLED, event.status());
        }

        @Test
        void shouldSendSingleEventWhenSettingsAreEnabled() {
            stubEnabledFields();

            support.applyCommonFields(USER_ID, settings, fields, SETTING_TYPE);

            verify(kafkaTemplate).send(anyString(), any());
        }

        @Test
        void shouldUpdateSettingsBeforeSendingEventWhenSettingsAreEnabled() {
            stubEnabledFields();

            support.applyCommonFields(USER_ID, settings, fields, SETTING_TYPE);

            InOrder inOrder = inOrder(settings, kafkaTemplate);
            inOrder.verify(settings).setNextExecutionDate(START_DATE);
            inOrder.verify(kafkaTemplate).send(eq(TOPIC), any());
        }

        @Test
        void shouldUpdateSettingsBeforeSendingEventWhenSettingsAreDisabled() {
            stubDisabledFields();

            support.applyCommonFields(USER_ID, settings, fields, SETTING_TYPE);

            InOrder inOrder = inOrder(settings, kafkaTemplate);
            inOrder.verify(settings).setNextExecutionDate(null);
            inOrder.verify(kafkaTemplate).send(eq(TOPIC), any());
        }

        @Test
        void shouldThrowExceptionWhenFieldsAreNull() {
            assertThrows(NullPointerException.class, () -> support.applyCommonFields(USER_ID, settings, null, SETTING_TYPE));

            verifyNoInteractions(settings, kafkaTemplate);
        }

        @Test
        void shouldThrowExceptionWhenSettingsAreNull() {
            when(fields.enable()).thenReturn(true);

            assertThrows(NullPointerException.class, () -> support.applyCommonFields(USER_ID, null, fields, SETTING_TYPE));

            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        void shouldThrowExceptionWhenKafkaSendFailsForEnabledSettings() {
            stubEnabledFields();
            doThrow(new IllegalStateException("kafka down")).when(kafkaTemplate).send(eq(TOPIC), any());

            assertThrows(IllegalStateException.class,
                    () -> support.applyCommonFields(USER_ID, settings, fields, SETTING_TYPE));

            verify(settings).setNextExecutionDate(START_DATE);
        }

        @Test
        void shouldThrowExceptionWhenKafkaSendFailsForDisabledSettings() {
            stubDisabledFields();
            doThrow(new IllegalStateException("kafka down")).when(kafkaTemplate).send(eq(TOPIC), any());

            assertThrows(IllegalStateException.class,
                    () -> support.applyCommonFields(USER_ID, settings, fields, SETTING_TYPE));

            verify(settings).setNextExecutionDate(null);
        }
    }
}