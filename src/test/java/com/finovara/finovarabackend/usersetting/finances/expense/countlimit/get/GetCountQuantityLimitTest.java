package com.finovara.finovarabackend.usersetting.finances.expense.countlimit.get;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.service.CountQuantityLimitService;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCountQuantityLimitTest {

    @Mock
    private UserManagerService userManagerService;

    @InjectMocks
    private CountQuantityLimitService countQuantityLimitService;

    private User user;
    private ExpenseSettings expenseSettings;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(USER_ID);
        expenseSettings = new ExpenseSettings();
        user.setExpenseSettings(expenseSettings);
    }

    @Test
    void shouldReturnEnabledLimit() {
        expenseSettings.setCountQuantityLimitEnabled(true);
        expenseSettings.setPeriodType(PeriodType.DAILY);
        expenseSettings.setNumberOfQuantityLimit(5);

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

        CountQuantityLimitDto dto = countQuantityLimitService.getCountQuantityLimit(USER_ID);

        assertEquals(true, dto.expenseCountLimitEnabled());
        assertEquals(PeriodType.DAILY, dto.periodType());
        assertEquals(5, dto.numberOfQuantityLimit());
    }

    @Test
    void shouldReturnDisabledLimit() {
        expenseSettings.setCountQuantityLimitEnabled(false);
        expenseSettings.setPeriodType(PeriodType.WEEKLY);
        expenseSettings.setNumberOfQuantityLimit(10);

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

        CountQuantityLimitDto dto = countQuantityLimitService.getCountQuantityLimit(USER_ID);

        assertEquals(false, dto.expenseCountLimitEnabled());
        assertEquals(PeriodType.WEEKLY, dto.periodType());
        assertEquals(10, dto.numberOfQuantityLimit());
    }
}