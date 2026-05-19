package com.finovara.finovarabackend.usersetting.finances.recurring.service.transaction;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.recurring.dto.RecurringCommonFields;
import com.finovara.finovarabackend.usersetting.finances.recurring.dto.RecurringSavingsDto;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringType;
import com.finovara.finovarabackend.usersetting.finances.recurring.service.support.RecurringSettingsSupport;
import com.finovara.finovarabackend.usersetting.finances.recurring.service.validator.RecurringSavingsValidator;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.wallet.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringSavingsServiceTest {

    @Mock
    private RecurringSettingsSupport recurringSettingsSupport;

    @Mock
    private RecurringSavingsValidator recurringSavingsValidator;

    @InjectMocks
    private RecurringSavingsService recurringSavingsService;

    private Long userId;
    private RecurringSettings settings;

    @BeforeEach
    void setUp() {
        userId = 1L;
        settings = new RecurringSettings();

        User user = new User();
        Wallet wallet = Wallet.create(user);
        user.setWallet(wallet);

        settings.setUserAssigned(user);
    }

    @Nested
    class SaveSavingsSettings {

        @Test
        void shouldSaveAndValidateWhenEnabled() {
            RecurringSavingsDto dto = new RecurringSavingsDto(
                    true,
                    BigDecimal.valueOf(100),
                    10L,
                    PeriodType.MONTHLY,
                    LocalDate.of(2025, 1, 1),
                    null
            );

            settings.setEnable(true);

            when(recurringSettingsSupport.getSettings(userId, RecurringType.SAVINGS)).thenReturn(settings);

            recurringSavingsService.saveSavingsSettings(userId, dto);

            assertEquals(dto.piggyBankId(), settings.getPiggyBankId());
            assertNull(settings.getExpenseCategory());
            assertNull(settings.getRevenueCategory());

            verify(recurringSettingsSupport).applyCommonFields(eq(userId), eq(settings), any(RecurringCommonFields.class),
                    eq(SettingType.SAVINGS_RECURRING));

            verify(recurringSavingsValidator).validate(settings, settings.getUserAssigned().getWallet());
        }

        @Test
        void shouldNotValidateWhenDisabled() {
            RecurringSavingsDto dto = new RecurringSavingsDto(
                    false,
                    BigDecimal.valueOf(100),
                    10L,
                    PeriodType.MONTHLY,
                    LocalDate.of(2025, 1, 1),
                    null
            );

            settings.setEnable(false);

            when(recurringSettingsSupport.getSettings(userId, RecurringType.SAVINGS)).thenReturn(settings);

            recurringSavingsService.saveSavingsSettings(userId, dto);

            verify(recurringSavingsValidator, never()).validate(any(), any());
        }
    }

    @Nested
    class GetSavingsSettings {

        @Test
        void shouldReturnDtoFromSettings() {
            settings.setEnable(true);
            settings.setAmount(BigDecimal.valueOf(200));
            settings.setPiggyBankId(10L);
            settings.setPeriodType(PeriodType.MONTHLY);
            settings.setStartDate(LocalDate.of(2025, 1, 1));
            settings.setNextExecutionDate(LocalDate.of(2025, 1, 2));

            when(recurringSettingsSupport.getSettings(userId, RecurringType.SAVINGS))
                    .thenReturn(settings);

            RecurringSavingsDto result = recurringSavingsService.getSavingsSettings(userId);

            assertEquals(settings.isEnable(), result.enable());
            assertEquals(settings.getAmount(), result.amount());
            assertEquals(settings.getPiggyBankId(), result.piggyBankId());
            assertEquals(settings.getPeriodType(), result.periodType());
            assertEquals(settings.getStartDate(), result.startDate());
            assertEquals(settings.getNextExecutionDate(), result.nextExecutionDate());
        }
    }
}