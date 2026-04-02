package com.finovara.finovarabackend.usersetting.finances.revenue.recurring.service.get;

import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.revenue.model.RevenueSettings;
import com.finovara.finovarabackend.usersetting.finances.revenue.recurring.dto.RecurringRevenueDto;
import com.finovara.finovarabackend.usersetting.finances.revenue.recurring.service.RecurringRevenueService;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetRecurringRevenueTest {

    @Mock
    private UserManagerService userManagerService;

    @InjectMocks
    private RecurringRevenueService recurringRevenueService;

    private RevenueSettings revenueSettings;
    private final String EMAIL = "test@test.com";

    @BeforeEach
    void setup() {
        User user = new User();
        revenueSettings = new RevenueSettings();
        user.setRevenueSettings(revenueSettings);

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
    }

    @Test
    void shouldReturnRecurringRevenueWhenEnabled() {
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        revenueSettings.setRecurringRevenuesEnable(true);
        revenueSettings.setRecurringAmount(BigDecimal.valueOf(500));
        revenueSettings.setRevenueCategory(RevenueCategory.SALARY);
        revenueSettings.setPeriodType(PeriodType.MONTHLY);
        revenueSettings.setRecurringStartDate(startDate);
        revenueSettings.setNextExecutionDate(startDate.plusDays(1));

        RecurringRevenueDto dto = recurringRevenueService.getRecurringRevenue(EMAIL);

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

        RecurringRevenueDto dto = recurringRevenueService.getRecurringRevenue(EMAIL);

        assertEquals(false, dto.recurringRevenueEnable());
        assertEquals(BigDecimal.valueOf(200), dto.amount());
        assertEquals(RevenueCategory.BONUS, dto.category());
        assertEquals(PeriodType.WEEKLY, dto.periodType());
        assertEquals(startDate, dto.startDate());
        assertNull(dto.nextExecutionDate());
    }
}