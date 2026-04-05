package com.finovara.finovarabackend.usersetting.finances.expense.countlimit;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitEmergencyModeDto;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.service.CountQuantityLimitEmergencyModeService;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
class CountQuantityLimitEmergencyModeTest {

    @Mock
    private UserManagerService userManagerService;

    @InjectMocks
    private CountQuantityLimitEmergencyModeService emergencyModeService;

    private User user;
    private ExpenseSettings expenseSettings;

    private final String EMAIL = "test@test.com";

    @BeforeEach
    void setup() {
        user = new User();
        expenseSettings = new ExpenseSettings();
        user.setExpenseSettings(expenseSettings);
    }

    @Test
    void shouldEnableEmergencyMode() {
        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);

        CountQuantityLimitEmergencyModeDto dto = new CountQuantityLimitEmergencyModeDto(true);

        emergencyModeService.saveEmergencyMode(EMAIL, dto);

        assertTrue(expenseSettings.isQuantityLimitEmergencyModeEnabled());
    }

    @Test
    void shouldDisableEmergencyMode() {
        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);

        CountQuantityLimitEmergencyModeDto dto = new CountQuantityLimitEmergencyModeDto(false);

        emergencyModeService.saveEmergencyMode(EMAIL, dto);

        assertFalse(expenseSettings.isQuantityLimitEmergencyModeEnabled());
    }
}