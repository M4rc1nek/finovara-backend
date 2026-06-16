package com.finovara.financeservice.settings.finances.expense.countlimit.service;

import com.finovara.financeservice.settings.finances.expense.countlimit.dto.CountQuantityLimitEmergencyModeDto;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.repository.ExpenseSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CountQuantityLimitEmergencyModeTest {

    @Mock
    private ExpenseSettingsRepository expenseSettingsRepository;

    @InjectMocks
    private CountQuantityLimitEmergencyModeService emergencyModeService;

    private ExpenseSettings expenseSettings;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setup() {
        expenseSettings = new ExpenseSettings();
        when(expenseSettingsRepository.findByUserId(USER_ID)).thenReturn(expenseSettings);
    }

    @ParameterizedTest
    @CsvSource({
            "true",
            "false"
    })
    void shouldSetEmergencyModeBasedOnDto(boolean enabled) {
        CountQuantityLimitEmergencyModeDto dto =
                new CountQuantityLimitEmergencyModeDto(enabled);

        emergencyModeService.saveEmergencyMode(USER_ID, dto);

        assertEquals(enabled, expenseSettings.isQuantityLimitEmergencyModeEnabled());
    }
}
