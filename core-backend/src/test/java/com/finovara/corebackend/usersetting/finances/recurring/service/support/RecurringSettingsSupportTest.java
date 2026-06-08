package com.finovara.corebackend.usersetting.finances.recurring.service.support;

import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.contracts.event.activity.settings.SettingsActivityEvent;
import com.finovara.corebackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.corebackend.usersetting.finances.recurring.model.RecurringType;
import com.finovara.corebackend.usersetting.finances.recurring.repository.RecurringSettingsRepository;
import com.finovara.corebackend.usersetting.finances.recurring.dto.RecurringCommonFields;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.kafka.core.KafkaTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringSettingsSupportTest {

    @Mock
    private RecurringSettingsRepository recurringSettingsRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private RecurringSettingsSupport recurringSettingsSupport;

    private RecurringSettings settings;

    @BeforeEach
    void setUp() {
        settings = new RecurringSettings();
    }

    @Test
    void shouldReturnSettings() {
        Long userId = 1L;

        when(recurringSettingsRepository.findByUserAssignedIdAndType(userId, RecurringType.REVENUE)).
                thenReturn(Optional.of(settings));

        RecurringSettings result = recurringSettingsSupport.getSettings(userId, RecurringType.REVENUE);

        assertEquals(settings, result);

        verify(recurringSettingsRepository).findByUserAssignedIdAndType(userId, RecurringType.REVENUE);
    }

    @Test
    void shouldApplyCommonFieldsWhenEnabledUsingSettingType() {
        Long userId = 1L;

        RecurringCommonFields fields = new RecurringCommonFields(
                true,
                BigDecimal.valueOf(100),
                null,
                LocalDate.of(2025, 1, 1)
        );

        recurringSettingsSupport.applyCommonFields(userId, settings, fields, SettingType.EXPENSE_RECURRING);

        assertTrue(settings.isEnable());
        assertEquals(BigDecimal.valueOf(100), settings.getAmount());
        assertEquals(LocalDate.of(2025, 1, 1), settings.getStartDate());
        assertEquals(LocalDate.of(2025, 1, 1), settings.getNextExecutionDate());

        ArgumentCaptor<SettingsActivityEvent> eventCaptor = ArgumentCaptor.forClass(SettingsActivityEvent.class);
        verify(kafkaTemplate).send(eq("activity.settings"), eventCaptor.capture());
        assertEquals(SettingActivityStatus.ENABLED, eventCaptor.getValue().status());
    }

    @Test
    void shouldApplyCommonFieldsWhenDisabledUsingSettingType() {
        Long userId = 1L;

        RecurringCommonFields fields = new RecurringCommonFields(
                false,
                BigDecimal.valueOf(50),
                null,
                LocalDate.of(2025, 1, 1)
        );

        recurringSettingsSupport.applyCommonFields(userId, settings, fields, SettingType.REVENUE_RECURRING);

        assertFalse(settings.isEnable());
        assertEquals(BigDecimal.valueOf(50), settings.getAmount());
        assertNull(settings.getNextExecutionDate());

        ArgumentCaptor<SettingsActivityEvent> eventCaptor = ArgumentCaptor.forClass(SettingsActivityEvent.class);
        verify(kafkaTemplate).send(eq("activity.settings"), eventCaptor.capture());
        assertEquals(SettingActivityStatus.DISABLED, eventCaptor.getValue().status());
    }

    @Test
    void shouldThrowExceptionWhenSettingsNotFound() {
        Long userId = 1L;

        when(recurringSettingsRepository.findByUserAssignedIdAndType(userId, RecurringType.EXPENSE)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> recurringSettingsSupport.getSettings(userId, RecurringType.EXPENSE));

        verify(recurringSettingsRepository).findByUserAssignedIdAndType(userId, RecurringType.EXPENSE);
    }
}