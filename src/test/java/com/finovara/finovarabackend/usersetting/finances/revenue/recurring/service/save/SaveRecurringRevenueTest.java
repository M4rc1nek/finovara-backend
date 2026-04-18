package com.finovara.finovarabackend.usersetting.finances.revenue.recurring.service.save;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.revenue.model.RevenueSettings;
import com.finovara.finovarabackend.usersetting.finances.revenue.recurring.dto.RecurringRevenueDto;
import com.finovara.finovarabackend.usersetting.finances.revenue.recurring.service.RecurringRevenueService;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveRecurringRevenueTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private SettingsActivityService settingsActivityService;

    @InjectMocks
    private RecurringRevenueService recurringRevenueService;

    private RevenueSettings revenueSettings;
    private final Long USER_ID = 1L;

    @BeforeEach
    void setup() {
        User user = new User();
        revenueSettings = new RevenueSettings();
        user.setRevenueSettings(revenueSettings);

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
    }

    @Test
    void shouldEnableRecurringRevenue() {
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        RecurringRevenueDto dto = new RecurringRevenueDto(
                true,
                BigDecimal.valueOf(500),
                RevenueCategory.SALARY,
                PeriodType.MONTHLY,
                startDate,
                null
        );

        recurringRevenueService.saveRecurringRevenue(USER_ID, dto);

        assertTrue(revenueSettings.isRecurringRevenuesEnable());
        assertEquals(BigDecimal.valueOf(500), revenueSettings.getRecurringAmount());
        assertEquals(RevenueCategory.SALARY, revenueSettings.getRevenueCategory());
        assertEquals(PeriodType.MONTHLY, revenueSettings.getPeriodType());
        assertEquals(startDate, revenueSettings.getRecurringStartDate());
        assertEquals(startDate, revenueSettings.getNextExecutionDate());

        verify(settingsActivityService).createSettingActivity(USER_ID, SettingActivityStatus.ENABLED, SettingType.REVENUE_RECURRING);
    }

    @Test
    void shouldDisableRecurringRevenue() {
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        RecurringRevenueDto dto = new RecurringRevenueDto(
                false,
                BigDecimal.valueOf(500),
                RevenueCategory.SALARY,
                PeriodType.MONTHLY,
                startDate,
                null
        );

        recurringRevenueService.saveRecurringRevenue(USER_ID, dto);

        assertFalse(revenueSettings.isRecurringRevenuesEnable());
        assertEquals(BigDecimal.valueOf(500), revenueSettings.getRecurringAmount());
        assertEquals(RevenueCategory.SALARY, revenueSettings.getRevenueCategory());
        assertEquals(PeriodType.MONTHLY, revenueSettings.getPeriodType());
        assertNull(revenueSettings.getNextExecutionDate());

        verify(settingsActivityService).createSettingActivity(USER_ID, SettingActivityStatus.DISABLED, SettingType.REVENUE_RECURRING);
    }
}