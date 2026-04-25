package com.finovara.finovarabackend.usersetting.finances.revenue.recurring.service;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingActivityStatus;
import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.accountactivity.settings.service.SettingsActivityService;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.revenue.model.RevenueSettings;
import com.finovara.finovarabackend.usersetting.finances.revenue.recurring.dto.RecurringRevenueDto;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
class RecurringRevenueServiceTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private SettingsActivityService settingsActivityService;

    @InjectMocks
    private RecurringRevenueService recurringRevenueService;

    private User user;
    private RevenueSettings revenueSettings;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setup() {
        user = new User();
        revenueSettings = new RevenueSettings();
        user.setRevenueSettings(revenueSettings);

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
    }

    @Nested
    class SaveRecurringRevenueTest {

        @Test
        void shouldEnableRecurringRevenue() {
            LocalDate startDate = LocalDate.of(2026, 3, 1);

            RecurringRevenueDto dto = new RecurringRevenueDto(true, BigDecimal.valueOf(500), RevenueCategory.SALARY, PeriodType.MONTHLY, startDate, null);

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

            RecurringRevenueDto dto = new RecurringRevenueDto(false, BigDecimal.valueOf(500), RevenueCategory.SALARY, PeriodType.MONTHLY, startDate, null);

            recurringRevenueService.saveRecurringRevenue(USER_ID, dto);

            assertFalse(revenueSettings.isRecurringRevenuesEnable());
            assertEquals(BigDecimal.valueOf(500), revenueSettings.getRecurringAmount());
            assertEquals(RevenueCategory.SALARY, revenueSettings.getRevenueCategory());
            assertEquals(PeriodType.MONTHLY, revenueSettings.getPeriodType());
            assertNull(revenueSettings.getNextExecutionDate());

            verify(settingsActivityService).createSettingActivity(USER_ID, SettingActivityStatus.DISABLED, SettingType.REVENUE_RECURRING);
        }
    }

    @Nested
    class GetRecurringRevenueTest {
        @Test
        void shouldReturnRecurringRevenueWhenEnabled() {
            LocalDate startDate = LocalDate.of(2026, 3, 1);

            revenueSettings.setRecurringRevenuesEnable(true);
            revenueSettings.setRecurringAmount(BigDecimal.valueOf(500));
            revenueSettings.setRevenueCategory(RevenueCategory.SALARY);
            revenueSettings.setPeriodType(PeriodType.MONTHLY);
            revenueSettings.setRecurringStartDate(startDate);
            revenueSettings.setNextExecutionDate(startDate.plusDays(1));

            RecurringRevenueDto dto = recurringRevenueService.getRecurringRevenue(USER_ID);

            assertTrue(dto.recurringRevenueEnable());
            assertEquals(BigDecimal.valueOf(500), dto.amount());
            assertEquals(RevenueCategory.SALARY, dto.category());
            assertEquals(PeriodType.MONTHLY, dto.periodType());
            assertEquals(startDate, dto.startDate());
            assertEquals(startDate.plusDays(1), dto.nextExecutionDate());
        }

        @Test
        void shouldReturnRecurringRevenueWhenDisabled() {
            LocalDate startDate = LocalDate.of(2026, 3, 1);

            revenueSettings.setRecurringRevenuesEnable(false);
            revenueSettings.setRecurringAmount(BigDecimal.valueOf(200));
            revenueSettings.setRevenueCategory(RevenueCategory.BONUS);
            revenueSettings.setPeriodType(PeriodType.WEEKLY);
            revenueSettings.setRecurringStartDate(startDate);
            revenueSettings.setNextExecutionDate(null);

            RecurringRevenueDto dto = recurringRevenueService.getRecurringRevenue(USER_ID);

            assertFalse(dto.recurringRevenueEnable());
            assertEquals(BigDecimal.valueOf(200), dto.amount());
            assertEquals(RevenueCategory.BONUS, dto.category());
            assertEquals(PeriodType.WEEKLY, dto.periodType());
            assertEquals(startDate, dto.startDate());
            assertNull(dto.nextExecutionDate());
        }
    }
}