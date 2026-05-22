package com.finovara.finovarabackend.usersetting.finances.recurring.service.transaction;

import com.finovara.finovarabackend.accountactivity.settings.model.SettingType;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.dto.RecurringExpenseDto;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringType;
import com.finovara.finovarabackend.usersetting.finances.recurring.dto.RecurringCommonFields;
import com.finovara.finovarabackend.usersetting.finances.recurring.service.support.RecurringSettingsSupport;
import com.finovara.finovarabackend.usersetting.finances.recurring.service.validator.RecurringExpenseValidator;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.wallet.model.Wallet;
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
class RecurringExpenseServiceTest {

    @Mock
    private RecurringSettingsSupport recurringSettingsSupport;

    @Mock
    private RecurringExpenseValidator recurringExpenseValidator;

    @InjectMocks
    private RecurringExpenseService recurringExpenseService;

    private Long userId;
    private RecurringSettings settings;

    @BeforeEach
    void setUp() {
        userId = 1L;
        settings = new RecurringSettings();

        User user = new User();
        user.setExpenseSettings(new ExpenseSettings());
        user.setWallet(Wallet.create(user));

        settings.setUserAssigned(user);
    }


    @Nested
    class SaveExpenseSettings {

        @Test
        void shouldSaveAndValidateWhenEnabled() {
            RecurringExpenseDto dto = new RecurringExpenseDto(
                    true,
                    BigDecimal.valueOf(100),
                    null,
                    PeriodType.MONTHLY,
                    LocalDate.of(2025, 1, 1),
                    null
            );

            settings.setEnable(true);

            when(recurringSettingsSupport.getSettings(userId, RecurringType.EXPENSE)).thenReturn(settings);

            recurringExpenseService.saveExpenseSettings(userId, dto);

            assertEquals(dto.expenseCategory(), settings.getExpenseCategory());
            assertNull(settings.getRevenueCategory());
            assertNull(settings.getPiggyBankId());

            verify(recurringSettingsSupport).applyCommonFields(eq(userId), eq(settings), any(RecurringCommonFields.class), eq(SettingType.EXPENSE_RECURRING));

            verify(recurringExpenseValidator).validate(eq(settings), any(ExpenseSettings.class), any(Wallet.class));
        }

        @Test
        void shouldNotValidateWhenDisabled() {
            RecurringExpenseDto dto = new RecurringExpenseDto(
                    false,
                    BigDecimal.valueOf(100),
                    null,
                    PeriodType.MONTHLY,
                    LocalDate.of(2025, 1, 1),
                    null
            );

            settings.setEnable(false);

            when(recurringSettingsSupport.getSettings(userId, RecurringType.EXPENSE)).thenReturn(settings);

            recurringExpenseService.saveExpenseSettings(userId, dto);

            verify(recurringExpenseValidator, never()).validate(any(), any(), any());
        }
    }


    @Nested
    class GetExpenseSettings {

        @Test
        void shouldReturnDtoFromSettings() {
            settings.setEnable(true);
            settings.setAmount(BigDecimal.valueOf(200));
            settings.setExpenseCategory(null);
            settings.setPeriodType(PeriodType.MONTHLY);
            settings.setStartDate(LocalDate.of(2025, 1, 1));
            settings.setNextExecutionDate(LocalDate.of(2025, 1, 2));

            when(recurringSettingsSupport.getSettings(userId, RecurringType.EXPENSE))
                    .thenReturn(settings);

            RecurringExpenseDto result = recurringExpenseService.getExpenseSettings(userId);

            assertEquals(settings.isEnable(), result.enable());
            assertEquals(settings.getAmount(), result.amount());
            assertEquals(settings.getExpenseCategory(), result.expenseCategory());
            assertEquals(settings.getPeriodType(), result.periodType());
            assertEquals(settings.getStartDate(), result.startDate());
            assertEquals(settings.getNextExecutionDate(), result.nextExecutionDate());
        }
    }
}