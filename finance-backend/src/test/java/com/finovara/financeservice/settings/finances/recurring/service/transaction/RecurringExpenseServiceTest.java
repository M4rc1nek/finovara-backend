package com.finovara.financeservice.settings.finances.recurring.service.transaction;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.repository.ExpenseSettingsRepository;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringCommonFields;
import com.finovara.financeservice.settings.finances.recurring.dto.RecurringExpenseDto;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringType;
import com.finovara.financeservice.settings.finances.recurring.service.support.RecurringSettingsSupport;
import com.finovara.financeservice.settings.finances.recurring.service.validator.ExpenseSettingsValidator;
import com.finovara.financeservice.util.limit.manager.LimitManagerService;
import  com.finovara.financeservice.feignclient.AuthBackendClient;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringExpenseServiceTest {

    @Mock
    private RecurringSettingsSupport recurringSettingsSupport;

    @Mock
    private ExpenseSettingsValidator expenseSettingsValidator;

    @Mock
    private ExpenseSettingsRepository expenseSettingsRepository;

    @Mock
    private WalletManagerService walletManagerService;

    @Mock
    private LimitManagerService limitManagerService;

    @Mock
    private AuthBackendClient authBackendClient;

    @InjectMocks
    private RecurringExpenseService recurringExpenseService;

    private Long userId;

    private RecurringSettings settings;

    @BeforeEach
    void setUp() {
        userId = 1L;
        settings = new RecurringSettings();
        settings.setUserId(userId);
    }

    @Nested
    class SaveExpenseSettingsTests {

        private RecurringExpenseDto dto;

        @BeforeEach
        void setUp() {
            dto = new RecurringExpenseDto(
                    true,
                    BigDecimal.valueOf(100),
                    ExpenseCategory.FOOD,
                    PeriodType.MONTHLY,
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2025, 2, 1),
                    "1234"
            );
        }

        @Test
        void shouldSaveExpenseCategoryAndClearOtherCategoriesWhenSaving() {
            when(recurringSettingsSupport.getSettings(userId, RecurringType.EXPENSE)).thenReturn(settings);

            recurringExpenseService.saveExpenseSettings(userId, dto);

            assertEquals(dto.expenseCategory(), settings.getExpenseCategory());
            assertNull(settings.getRevenueCategory());
            assertNull(settings.getPiggyBankId());
        }

        @Test
        void shouldApplyCommonFieldsWhenSaving() {
            when(recurringSettingsSupport.getSettings(userId, RecurringType.EXPENSE)).thenReturn(settings);

            recurringExpenseService.saveExpenseSettings(userId, dto);

            ArgumentCaptor<RecurringCommonFields> captor = ArgumentCaptor.forClass(RecurringCommonFields.class);
            verify(recurringSettingsSupport).applyCommonFields(eq(userId), eq(settings), captor.capture(), eq(SettingType.EXPENSE_RECURRING));
            RecurringCommonFields capturedFields = captor.getValue();
            assertThat(capturedFields.enable()).isEqualTo(dto.enable());
            assertThat(capturedFields.amount()).isEqualByComparingTo(dto.amount());
            assertThat(capturedFields.periodType()).isEqualTo(dto.periodType());
            assertThat(capturedFields.startDate()).isEqualTo(dto.startDate());
            assertThat(capturedFields.endDate()).isEqualTo(dto.endDate());
        }

        @Test
        void shouldValidateWhenEnabled() {
            settings.setEnable(true);
            ExpenseSettings expenseSettings = mock(ExpenseSettings.class);
            Wallet wallet = Wallet.create(userId);
            List<Limit> limits = List.of();

            when(recurringSettingsSupport.getSettings(userId, RecurringType.EXPENSE)).thenReturn(settings);
            when(expenseSettingsRepository.findByUserId(userId)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(userId)).thenReturn(wallet);
            when(limitManagerService.getLimitsByUserId(userId)).thenReturn(limits);

            recurringExpenseService.saveExpenseSettings(userId, dto);

            verify(expenseSettingsValidator, times(1)).validate(settings, expenseSettings, wallet, limits);
        }

        @Test
        void shouldNotValidateWhenDisabled() {
            RecurringExpenseDto disabledDto = new RecurringExpenseDto(
                    false,
                    BigDecimal.valueOf(100),
                    ExpenseCategory.FOOD,
                    PeriodType.MONTHLY,
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2025, 2, 1),
                    "1234"
            );

            when(recurringSettingsSupport.getSettings(userId, RecurringType.EXPENSE)).thenReturn(settings);

            recurringExpenseService.saveExpenseSettings(userId, disabledDto);

            verify(expenseSettingsValidator, never()).validate(any(), any(), any(), any());
        }

        @Test
        void shouldNotFetchExpenseSettingsWalletOrLimitsWhenDisabled() {
            RecurringExpenseDto disabledDto = new RecurringExpenseDto(
                    false,
                    BigDecimal.valueOf(100),
                    ExpenseCategory.FOOD,
                    PeriodType.MONTHLY,
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2025, 2, 1),
                    null
            );

            when(recurringSettingsSupport.getSettings(userId, RecurringType.EXPENSE)).thenReturn(settings);

            recurringExpenseService.saveExpenseSettings(userId, disabledDto);

            verifyNoInteractions(expenseSettingsRepository, walletManagerService, limitManagerService);
        }
    }

    @Nested
    class GetExpenseSettingsTests {

        @Test
        void shouldReturnDtoWhenSettingsExist() {
            settings.setEnable(true);
            settings.setAmount(BigDecimal.valueOf(200));
            settings.setExpenseCategory(ExpenseCategory.TRANSPORT);
            settings.setPeriodType(PeriodType.MONTHLY);
            settings.setStartDate(LocalDate.of(2025, 1, 1));
            settings.setEndDate(LocalDate.of(2026, 1, 1));
            settings.setNextExecutionDate(LocalDate.of(2025, 1, 2));

            when(recurringSettingsSupport.getSettings(userId, RecurringType.EXPENSE)).thenReturn(settings);

            RecurringExpenseDto result = recurringExpenseService.getExpenseSettings(userId);

            assertEquals(settings.isEnable(), result.enable());
            assertEquals(settings.getAmount(), result.amount());
            assertEquals(settings.getExpenseCategory(), result.expenseCategory());
            assertEquals(settings.getPeriodType(), result.periodType());
            assertEquals(settings.getStartDate(), result.startDate());
            assertEquals(settings.getEndDate(), result.endDate());
            assertEquals(settings.getNextExecutionDate(), result.nextExecutionDate());
        }

        @Test
        void shouldReturnDtoWithNullExpenseCategoryWhenNotSet() {
            settings.setEnable(false);
            settings.setAmount(BigDecimal.valueOf(50));
            settings.setExpenseCategory(null);
            settings.setPeriodType(PeriodType.WEEKLY);
            settings.setStartDate(LocalDate.of(2025, 2, 1));
            settings.setEndDate(null);
            settings.setNextExecutionDate(null);

            when(recurringSettingsSupport.getSettings(userId, RecurringType.EXPENSE)).thenReturn(settings);

            RecurringExpenseDto result = recurringExpenseService.getExpenseSettings(userId);

            assertNull(result.expenseCategory());
            assertNull(result.endDate());
            assertNull(result.nextExecutionDate());
        }
    }
}