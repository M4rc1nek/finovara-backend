package com.finovara.finovarabackend.usersetting.finances.expense.smartscan.service.save;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.dto.SmartScanDto;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.service.SmartScanService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveSmartScanTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private SettingsActivityService settingsActivityService;

    @InjectMocks
    private SmartScanService smartScanService;

    private ExpenseSettings expenseSettings;
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        User user = new User();
        expenseSettings = new ExpenseSettings();
        user.setExpenseSettings(expenseSettings);

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
    }

    @Test
    void shouldEnableSmartScan() {
        SmartScanDto dto = new SmartScanDto(true);

        smartScanService.saveSmartScan(USER_ID, dto);

        assertTrue(expenseSettings.isSmartScanEnabled());
        verify(settingsActivityService).createSettingActivity(USER_ID, SettingActivityStatus.ENABLED, SettingType.EXPENSE_SMART_SCAN);
    }

    @Test
    void shouldDisableSmartScan() {
        SmartScanDto dto = new SmartScanDto(false);

        smartScanService.saveSmartScan(USER_ID, dto);

        assertFalse(expenseSettings.isSmartScanEnabled());
        verify(settingsActivityService).createSettingActivity(USER_ID, SettingActivityStatus.DISABLED, SettingType.EXPENSE_SMART_SCAN);
    }
}