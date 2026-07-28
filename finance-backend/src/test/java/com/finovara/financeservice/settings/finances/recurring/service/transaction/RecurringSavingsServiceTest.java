package com.finovara.financeservice.settings.finances.recurring.service.transaction;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringCommonFields;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringSavingsDto;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringType;
import com.finovara.financeservice.settings.finances.recurring.service.occurrence.RecurringOccurrenceService;
import com.finovara.financeservice.settings.finances.recurring.service.support.RecurringSettingsSupport;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringSavingsValidator;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringSavingsServiceTest {

    @Mock
    private RecurringSettingsSupport recurringSettingsSupport;

    @Mock
    private RecurringSavingsValidator recurringSavingsValidator;

    @Mock
    private WalletManagerService walletManagerService;

    @Mock
    private RecurringOccurrenceService recurringOccurrenceService;

    @InjectMocks
    private RecurringSavingsService recurringSavingsService;

    private Long userId;
    private RecurringSettings settings;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        userId = 1L;
        settings = new RecurringSettings();
        settings.setUserId(userId);
        wallet = Wallet.create(userId);
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
                    LocalDate.of(2026, 1, 1),
                    null
            );

            settings.setEnable(true);

            when(recurringSettingsSupport.getSettings(userId, RecurringType.SAVINGS)).thenReturn(settings);
            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);

            recurringSavingsService.saveSavingsSettings(userId, dto);

            assertEquals(dto.piggyBankId(), settings.getPiggyBankId());
            assertNull(settings.getExpenseCategory());
            assertNull(settings.getRevenueCategory());

            ArgumentCaptor<RecurringCommonFields> captor = ArgumentCaptor.forClass(RecurringCommonFields.class);
            verify(recurringSettingsSupport).applyCommonFields(eq(userId), eq(settings), captor.capture(),
                    eq(SettingType.SAVINGS_RECURRING));
            assertThat(captor.getValue().endDate()).isEqualTo(dto.endDate());

            verify(recurringSavingsValidator).validate(settings, wallet);
        }

        @Test
        void shouldNotValidateWhenDisabled() {
            RecurringSavingsDto dto = new RecurringSavingsDto(
                    false,
                    BigDecimal.valueOf(100),
                    10L,
                    PeriodType.MONTHLY,
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2026, 1, 1),
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
            settings.setEndDate(LocalDate.of(2026, 1, 1));
            settings.setNextExecutionDate(LocalDate.of(2025, 1, 2));

            when(recurringSettingsSupport.getSettings(userId, RecurringType.SAVINGS)).thenReturn(settings);

            RecurringSavingsDto result = recurringSavingsService.getSavingsSettings(userId);

            assertEquals(settings.isEnable(), result.enable());
            assertEquals(settings.getAmount(), result.amount());
            assertEquals(settings.getPiggyBankId(), result.piggyBankId());
            assertEquals(settings.getPeriodType(), result.periodType());
            assertEquals(settings.getStartDate(), result.startDate());
            assertEquals(settings.getEndDate(), result.endDate());
            assertEquals(settings.getNextExecutionDate(), result.nextExecutionDate());
        }
    }
}