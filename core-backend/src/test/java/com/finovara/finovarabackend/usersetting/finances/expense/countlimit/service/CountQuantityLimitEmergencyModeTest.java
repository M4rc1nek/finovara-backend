package com.finovara.finovarabackend.usersetting.finances.expense.countlimit.service;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitEmergencyModeDto;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
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
    private UserManagerService userManagerService;

    @InjectMocks
    private CountQuantityLimitEmergencyModeService emergencyModeService;

    private User user;
    private ExpenseSettings expenseSettings;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setup() {
        user = new User();
        expenseSettings = new ExpenseSettings();
        user.setExpenseSettings(expenseSettings);
    }

    @ParameterizedTest
    @CsvSource({
            "true",
            "false"
    })
    void shouldSetEmergencyModeBasedOnDto(boolean enabled) {
        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

        CountQuantityLimitEmergencyModeDto dto =
                new CountQuantityLimitEmergencyModeDto(enabled);

        emergencyModeService.saveEmergencyMode(USER_ID, dto);

        assertEquals(enabled, expenseSettings.isQuantityLimitEmergencyModeEnabled());
    }
}