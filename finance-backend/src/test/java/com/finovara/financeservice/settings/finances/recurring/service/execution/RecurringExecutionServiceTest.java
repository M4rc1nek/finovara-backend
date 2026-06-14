package com.finovara.financeservice.settings.finances.recurring.service.execution;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.financeservice.expense.service.ExpenseService;
import com.finovara.financeservice.piggybank.service.PiggyBankTransactionService;
import com.finovara.financeservice.revenue.service.RevenueService;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.repository.ExpenseSettingsRepository;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringType;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringExpenseValidator;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringRevenueValidator;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringSavingsValidator;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringExecutionServiceTest {

    @Mock
    private RevenueService revenueService;
    @Mock
    private ExpenseService expenseService;
    @Mock
    private PiggyBankTransactionService piggyBankTransactionService;
    @Mock
    private RecurringExpenseValidator recurringExpenseValidator;
    @Mock
    private RecurringRevenueValidator recurringRevenueValidator;
    @Mock
    private RecurringSavingsValidator recurringSavingsValidator;
    @Mock
    private ExpenseSettingsRepository expenseSettingsRepository;
    @Mock
    private WalletManagerService walletManagerService;

    @InjectMocks
    private RecurringExecutionService recurringExecutionService;

    private static final Long USER_ID = 1L;

    private LocalDate date;
    private RecurringSettings settings;

    @BeforeEach
    void setUp() {
        date = LocalDate.of(2025, 1, 1);

        settings = new RecurringSettings();
        settings.setEnable(true);
        settings.setAmount(BigDecimal.valueOf(100));
        settings.setUserId(USER_ID);
        settings.setPeriodType(PeriodType.DAILY);
    }

    @Nested
    class Execute {

        @Test
        void shouldDoNothingWhenTypeIsNull() {
            settings.setType(null);

            recurringExecutionService.execute(settings, date);

            verifyNoInteractions(revenueService, expenseService, piggyBankTransactionService);
        }
    }

    @Nested
    class RecurringSkipCases {

        @ParameterizedTest
        @EnumSource(RecurringType.class)
        void shouldSkipWhenUserIsNull(RecurringType type) {
            settings.setType(type);
            settings.setUserId(null);

            recurringExecutionService.execute(settings, date);

            verifyNoInteractions(revenueService, expenseService, piggyBankTransactionService);
        }
    }

    @Nested
    class RecurringRevenue {

        @Test
        void shouldCreateRevenue() {
            settings.setType(RecurringType.REVENUE);
            settings.setRevenueCategory(RevenueCategory.SALARY);

            recurringExecutionService.execute(settings, date);

            verify(recurringRevenueValidator).validate(settings);
            verify(revenueService).addRevenue(any(), eq(USER_ID));
        }

        @Test
        void shouldSkipWhenRevenueCategoryIsNull() {
            settings.setType(RecurringType.REVENUE);
            settings.setRevenueCategory(null);

            recurringExecutionService.execute(settings, date);

            verifyNoInteractions(revenueService);
        }
    }

    @Nested
    class RecurringExpense {

        private ExpenseSettings expenseSettings;
        private Wallet wallet;

        @BeforeEach
        void setUpExpense() {
            expenseSettings = new ExpenseSettings();
            expenseSettings.setPeriodType(PeriodType.MONTHLY);
            expenseSettings.setCountQuantityLimitEnabled(true);
            expenseSettings.setNumberOfQuantityLimit(5);

            wallet = Wallet.create(USER_ID);

            settings.setType(RecurringType.EXPENSE);
            settings.setExpenseCategory(ExpenseCategory.FOOD);
        }

        @Test
        void shouldSkipWhenExpenseSettingsIsNull() {
            when(expenseSettingsRepository.findByUserId(USER_ID)).thenReturn(null);

            recurringExecutionService.execute(settings, date);

            verifyNoInteractions(expenseService);
        }

        @Test
        void shouldCreateExpense() {
            when(expenseSettingsRepository.findByUserId(USER_ID)).thenReturn(expenseSettings);
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(wallet);

            recurringExecutionService.execute(settings, date);

            verify(recurringExpenseValidator).validate(eq(settings), eq(expenseSettings), eq(wallet));
            verify(expenseService).addExpense(any(), eq(USER_ID), eq(PeriodType.MONTHLY));
        }
    }

    @Nested
    class RecurringSavings {

        @BeforeEach
        void setUpSavings() {
            settings.setType(RecurringType.SAVINGS);
            settings.setPiggyBankId(10L);
        }

        @Test
        void shouldSkipWhenUserIsNull() {
            settings.setUserId(null);

            recurringExecutionService.execute(settings, date);

            verifyNoInteractions(piggyBankTransactionService);
        }

        @Test
        void shouldCreateSavings() {
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(Wallet.create(USER_ID));

            recurringExecutionService.execute(settings, date);

            verify(recurringSavingsValidator).validate(eq(settings), any());
            verify(piggyBankTransactionService).addBalanceToPiggyBank(
                    eq(USER_ID),
                    eq(10L),
                    any(),
                    eq(PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING)
            );
        }

        @Test
        void shouldDisableWhenPiggyBankNotFound() {
            when(walletManagerService.getWalletByUserIdOrThrow(USER_ID)).thenReturn(Wallet.create(USER_ID));
            doThrow(new RequestedEntityNotFoundException("Piggy bank not found"))
                    .when(piggyBankTransactionService)
                    .addBalanceToPiggyBank(any(), any(), any(), any());

            recurringExecutionService.execute(settings, date);

            assertFalse(settings.isEnable());
            assertNull(settings.getNextExecutionDate());
        }
    }
}
