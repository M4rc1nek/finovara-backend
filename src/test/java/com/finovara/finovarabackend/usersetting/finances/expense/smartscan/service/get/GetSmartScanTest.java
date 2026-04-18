package com.finovara.finovarabackend.usersetting.finances.expense.smartscan.service.get;

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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSmartScanTest {

    @Mock
    private UserManagerService userManagerService;

    @InjectMocks
    private SmartScanService smartScanService;

    private ExpenseSettings expenseSettings;
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setup() {
        User user = new User();
        expenseSettings = new ExpenseSettings();
        user.setExpenseSettings(expenseSettings);

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
    }

    @Test
    void shouldReturnEnabledTrue() {
        expenseSettings.setSmartScanEnabled(true);

        SmartScanDto result = smartScanService.getSmartScan(USER_ID);

        assertTrue(result.smartScanEnabled());
    }

    @Test
    void shouldReturnEnabledFalse() {
        expenseSettings.setSmartScanEnabled(false);

        SmartScanDto result = smartScanService.getSmartScan(USER_ID);

        assertFalse(result.smartScanEnabled());
    }
}