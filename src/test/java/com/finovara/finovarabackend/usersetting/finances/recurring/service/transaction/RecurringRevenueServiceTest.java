package com.finovara.finovarabackend.usersetting.finances.recurring.service.transaction;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.usersetting.finances.recurring.dto.RecurringRevenueDto;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringType;
import com.finovara.finovarabackend.usersetting.finances.recurring.service.RecurringCommonFields;
import com.finovara.finovarabackend.usersetting.finances.recurring.service.support.RecurringSettingsSupport;
import com.finovara.finovarabackend.usersetting.finances.recurring.service.validator.RecurringRevenueValidator;
import com.finovara.finovarabackend.util.model.PeriodType;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringRevenueServiceTest {

    @Mock
    private RecurringSettingsSupport recurringSettingsSupport;

    @Mock
    private RecurringRevenueValidator recurringRevenueValidator;

    @InjectMocks
    private RecurringRevenueService recurringRevenueService;

    private Long userId;
    private RecurringSettings settings;

    @BeforeEach
    void setUp() {
        userId = 1L;
        settings = new RecurringSettings();
    }


    @Nested
    class SaveRevenueSettings {

        @Test
        void shouldSaveAndValidateWhenEnabled() {
            RecurringRevenueDto dto = new RecurringRevenueDto(
                    true,
                    BigDecimal.valueOf(100),
                    RevenueCategory.SALARY,
                    PeriodType.MONTHLY,
                    LocalDate.of(2025, 1, 1),
                    null
            );

            settings.setEnable(true);

            when(recurringSettingsSupport.getSettings(userId, RecurringType.REVENUE)).thenReturn(settings);

            recurringRevenueService.saveRevenueSettings(userId, dto);

            assertEquals(dto.revenueCategory(), settings.getRevenueCategory());
            assertNull(settings.getExpenseCategory());
            assertNull(settings.getPiggyBankId());

            verify(recurringSettingsSupport).applyCommonFields(eq(userId), eq(settings), any(RecurringCommonFields.class),
                    eq(SettingType.REVENUE_RECURRING));

            verify(recurringRevenueValidator).validate(settings);
        }

        @Test
        void shouldNotValidateWhenDisabled() {
            RecurringRevenueDto dto = new RecurringRevenueDto(
                    false,
                    BigDecimal.valueOf(100),
                    RevenueCategory.SALARY,
                    PeriodType.MONTHLY,
                    LocalDate.of(2025, 1, 1),
                    null
            );

            settings.setEnable(false);

            when(recurringSettingsSupport.getSettings(userId, RecurringType.REVENUE)).thenReturn(settings);

            recurringRevenueService.saveRevenueSettings(userId, dto);

            verify(recurringRevenueValidator, never()).validate(any());
        }
    }

    @Nested
    class GetRevenueSettings {

        @Test
        void shouldReturnDtoFromSettings() {
            settings.setEnable(true);
            settings.setAmount(BigDecimal.valueOf(200));
            settings.setRevenueCategory(RevenueCategory.SALARY);
            settings.setPeriodType(PeriodType.MONTHLY);
            settings.setStartDate(LocalDate.of(2025, 1, 1));
            settings.setNextExecutionDate(LocalDate.of(2025, 1, 2));

            when(recurringSettingsSupport.getSettings(userId, RecurringType.REVENUE)).thenReturn(settings);

            RecurringRevenueDto result = recurringRevenueService.getRevenueSettings(userId);

            assertEquals(settings.isEnable(), result.enable());
            assertEquals(settings.getAmount(), result.amount());
            assertEquals(settings.getRevenueCategory(), result.revenueCategory());
            assertEquals(settings.getPeriodType(), result.periodType());
            assertEquals(settings.getStartDate(), result.startDate());
            assertEquals(settings.getNextExecutionDate(), result.nextExecutionDate());
        }
    }
}