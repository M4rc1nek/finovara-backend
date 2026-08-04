package com.finovara.financeservice.settings.finances.recurring.service.transaction;

import com.finovara.contracts.auth.dto.ConfirmAuthorizationCodeDto;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringCommonFields;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringRevenueDto;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringType;
import com.finovara.financeservice.settings.finances.recurring.service.support.RecurringSettingsSupport;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringRevenueValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringRevenueServiceTest {

    @Mock
    private RecurringSettingsSupport recurringSettingsSupport;

    @Mock
    private RecurringRevenueValidator recurringRevenueValidator;

    @Mock
    private AuthBackendClient authBackendClient;

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
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2025, 2, 1),
                    "1234"
            );

            settings.setEnable(true);

            when(recurringSettingsSupport.getSettings(userId, RecurringType.REVENUE)).thenReturn(settings);

            recurringRevenueService.saveRevenueSettings(userId, dto);

            assertEquals(dto.revenueCategory(), settings.getRevenueCategory());
            assertNull(settings.getExpenseCategory());
            assertNull(settings.getPiggyBankId());

            ArgumentCaptor<RecurringCommonFields> captor = ArgumentCaptor.forClass(RecurringCommonFields.class);
            verify(recurringSettingsSupport).applyCommonFields(eq(userId), eq(settings), captor.capture(),
                    eq(SettingType.REVENUE_RECURRING));
            assertThat(captor.getValue().endDate()).isEqualTo(dto.endDate());

            verify(recurringRevenueValidator).validate(settings);
            verify(authBackendClient).confirmAuthorizationCode(userId, new ConfirmAuthorizationCodeDto("1234"));

        }

        @Test
        void shouldNotValidateWhenDisabled() {
            RecurringRevenueDto dto = new RecurringRevenueDto(
                    false,
                    BigDecimal.valueOf(100),
                    RevenueCategory.SALARY,
                    PeriodType.MONTHLY,
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2025, 2, 1),
                    "1234"
            );

            settings.setEnable(false);

            when(recurringSettingsSupport.getSettings(userId, RecurringType.REVENUE)).thenReturn(settings);

            recurringRevenueService.saveRevenueSettings(userId, dto);

            verify(recurringRevenueValidator, never()).validate(any());
            verify(authBackendClient).confirmAuthorizationCode(userId, new ConfirmAuthorizationCodeDto("1234"));

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
            settings.setEndDate(LocalDate.of(2026, 1, 1));
            settings.setNextExecutionDate(LocalDate.of(2025, 1, 2));

            when(recurringSettingsSupport.getSettings(userId, RecurringType.REVENUE)).thenReturn(settings);

            RecurringRevenueDto result = recurringRevenueService.getRevenueSettings(userId);

            assertEquals(settings.isEnable(), result.enable());
            assertEquals(settings.getAmount(), result.amount());
            assertEquals(settings.getRevenueCategory(), result.revenueCategory());
            assertEquals(settings.getPeriodType(), result.periodType());
            assertEquals(settings.getStartDate(), result.startDate());
            assertEquals(settings.getEndDate(), result.endDate());
            assertEquals(settings.getNextExecutionDate(), result.nextExecutionDate());
        }
    }
}