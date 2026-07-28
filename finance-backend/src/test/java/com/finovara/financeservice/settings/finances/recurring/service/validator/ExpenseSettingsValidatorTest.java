package com.finovara.financeservice.settings.finances.recurring.service.validator;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.financeservice.exception.conflict.ConfirmationRequiredException;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.financeservice.settings.finances.expense.smartscan.service.SmartScanService;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.service.validator.util.RecurringBasicValidator;
import com.finovara.financeservice.util.periodbalance.FinancialPeriodService;
import com.finovara.financeservice.wallet.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseSettingsValidatorTest {

    @Mock
    private SmartScanService smartScanService;

    @Mock
    private RecurringBasicValidator recurringBasicValidator;

    @Mock
    private FinancialPeriodService financialPeriodService;

    @InjectMocks
    private ExpenseSettingsValidator expenseSettingsValidator;

    private Long userId;

    private RecurringSettings settings;

    private ExpenseSettings expenseSettings;

    @BeforeEach
    void setUp() {
        userId = 1L;
        settings = new RecurringSettings();
        settings.setUserId(userId);
        settings.setAmount(BigDecimal.valueOf(100));
        settings.setExpenseCategory(ExpenseCategory.FOOD);
        settings.setPeriodType(PeriodType.MONTHLY);
        settings.setNextExecutionDate(LocalDate.now().plusMonths(1));
        expenseSettings = new ExpenseSettings();
    }

    private Wallet sufficientWallet() {
        Wallet wallet = mock(Wallet.class);
        when(wallet.getBalance()).thenReturn(BigDecimal.valueOf(1000));
        return wallet;
    }

    @Nested
    class ValidateBasicsTests {

        @Test
        void shouldCallBasicValidatorWhenValidating() {
            Wallet wallet = sufficientWallet();

            expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of());

            verify(recurringBasicValidator, times(1)).validateBasics(settings, settings.getExpenseCategory());
        }

        @Test
        void shouldThrowExceptionWhenBasicValidatorRejectsSettings() {
            Wallet wallet = mock(Wallet.class);
            doThrow(new InvalidInputException("invalid settings")).when(recurringBasicValidator).validateBasics(any(), any());

            assertThrows(InvalidInputException.class, () -> expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of()));

            verifyNoInteractions(smartScanService, financialPeriodService);
        }
    }

    @Nested
    class ValidateBalanceTests {

        @Test
        void shouldThrowExceptionWhenAmountExceedsWalletBalance() {
            settings.setAmount(BigDecimal.valueOf(200));
            Wallet wallet = mock(Wallet.class);
            when(wallet.getBalance()).thenReturn(BigDecimal.valueOf(100));

            assertThrows(InvalidInputException.class, () -> expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of()));
        }

        @Test
        void shouldPassWhenAmountEqualsWalletBalance() {
            settings.setAmount(BigDecimal.valueOf(100));
            Wallet wallet = mock(Wallet.class);
            when(wallet.getBalance()).thenReturn(BigDecimal.valueOf(100));

            assertDoesNotThrow(() -> expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of()));
        }

        @Test
        void shouldPassWhenAmountLessThanWalletBalance() {
            settings.setAmount(BigDecimal.valueOf(50));
            Wallet wallet = mock(Wallet.class);
            when(wallet.getBalance()).thenReturn(BigDecimal.valueOf(100));

            assertDoesNotThrow(() -> expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of()));
        }
    }

    @Nested
    class ValidateQuantityLimitTests {

        @Test
        void shouldSkipValidationWhenCountQuantityLimitDisabled() {
            expenseSettings.setCountQuantityLimitEnabled(false);
            Wallet wallet = sufficientWallet();

            assertDoesNotThrow(() -> expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of()));
        }

        @Test
        void shouldThrowExceptionWhenPlannedExecutionsExceedLimit() {
            expenseSettings.setCountQuantityLimitEnabled(true);
            expenseSettings.setNumberOfQuantityLimit(1);
            settings.setPeriodType(PeriodType.MONTHLY);
            settings.setNextExecutionDate(LocalDate.now().withDayOfMonth(1).minusMonths(1));
            Wallet wallet = sufficientWallet();

            assertThrows(InvalidInputException.class, () -> expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of()));
        }

        @Test
        void shouldPassWhenPlannedExecutionsWithinLimit() {
            expenseSettings.setCountQuantityLimitEnabled(true);
            expenseSettings.setNumberOfQuantityLimit(5);
            settings.setPeriodType(PeriodType.MONTHLY);
            settings.setNextExecutionDate(LocalDate.now().plusMonths(1));
            Wallet wallet = sufficientWallet();

            assertDoesNotThrow(() -> expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of()));
        }
    }

    @Nested
    class ValidateAmountThresholdTests {

        @Test
        void shouldSkipValidationWhenAmountThresholdDisabled() {
            expenseSettings.setAmountThresholdEnabled(false);
            Wallet wallet = sufficientWallet();

            assertDoesNotThrow(() -> expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of()));
        }

        @Test
        void shouldThrowExceptionWhenAmountExceedsBlockedAmount() {
            expenseSettings.setAmountThresholdEnabled(true);
            expenseSettings.setBlockedAmount(BigDecimal.valueOf(50));
            settings.setAmount(BigDecimal.valueOf(100));
            Wallet wallet = sufficientWallet();

            assertThrows(InvalidInputException.class, () -> expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of()));
        }

        @Test
        void shouldPassWhenAmountEqualsBlockedAmount() {
            expenseSettings.setAmountThresholdEnabled(true);
            expenseSettings.setBlockedAmount(BigDecimal.valueOf(100));
            settings.setAmount(BigDecimal.valueOf(100));
            Wallet wallet = sufficientWallet();

            assertDoesNotThrow(() -> expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of()));
        }

        @Test
        void shouldPassWhenAmountBelowBlockedAmount() {
            expenseSettings.setAmountThresholdEnabled(true);
            expenseSettings.setBlockedAmount(BigDecimal.valueOf(200));
            settings.setAmount(BigDecimal.valueOf(100));
            Wallet wallet = sufficientWallet();

            assertDoesNotThrow(() -> expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of()));
        }
    }

    @Nested
    class ValidateSmartScanTests {

        @Test
        void shouldSkipSmartScanValidationWhenDisabled() {
            expenseSettings.setSmartScanEnabled(false);
            Wallet wallet = sufficientWallet();

            expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of());

            verifyNoInteractions(smartScanService);
        }

        @Test
        void shouldCallSmartScanServiceWhenEnabled() {
            expenseSettings.setSmartScanEnabled(true);
            Wallet wallet = sufficientWallet();

            expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of());

            verify(smartScanService, times(1)).handleSmartScan(userId, null, settings.getAmount(), SmartScanMode.ADD);
        }

        @Test
        void shouldThrowExceptionWhenSmartScanConfirmationRequired() {
            expenseSettings.setSmartScanEnabled(true);
            Wallet wallet = sufficientWallet();
            doThrow(new ConfirmationRequiredException("confirmation required"))
                    .when(smartScanService).handleSmartScan(userId, null, settings.getAmount(), SmartScanMode.ADD);

            assertThrows(InvalidInputException.class, () -> expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of()));
        }
    }

    @Nested
    class ValidateLimitsTests {

        @Test
        void shouldSkipLimitWhenCategoryDoesNotMatch() {
            Limit limit = new Limit();
            limit.setCategory(ExpenseCategory.TRANSPORT);
            limit.setPeriodType(PeriodType.MONTHLY);
            limit.setAmount(BigDecimal.valueOf(10));
            Wallet wallet = sufficientWallet();

            assertDoesNotThrow(() -> expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of(limit)));

            verifyNoInteractions(financialPeriodService);
        }

        @Test
        void shouldThrowExceptionWhenGeneralLimitExceeded() {
            Limit limit = new Limit();
            limit.setCategory(null);
            limit.setPeriodType(PeriodType.MONTHLY);
            limit.setAmount(BigDecimal.valueOf(150));
            settings.setAmount(BigDecimal.valueOf(100));
            Wallet wallet = sufficientWallet();
            when(financialPeriodService.getExpensesSum(userId, PeriodType.MONTHLY, null)).thenReturn(BigDecimal.valueOf(100));

            assertThrows(InvalidInputException.class, () -> expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of(limit)));
        }

        @Test
        void shouldThrowExceptionWhenCategoryLimitExceeded() {
            Limit limit = new Limit();
            limit.setCategory(ExpenseCategory.FOOD);
            limit.setPeriodType(PeriodType.MONTHLY);
            limit.setAmount(BigDecimal.valueOf(150));
            settings.setExpenseCategory(ExpenseCategory.FOOD);
            settings.setAmount(BigDecimal.valueOf(100));
            Wallet wallet = sufficientWallet();
            when(financialPeriodService.getExpensesSum(userId, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(BigDecimal.valueOf(100));

            assertThrows(InvalidInputException.class, () -> expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of(limit)));
        }

        @Test
        void shouldPassWhenTotalWithinLimit() {
            Limit limit = new Limit();
            limit.setCategory(ExpenseCategory.FOOD);
            limit.setPeriodType(PeriodType.MONTHLY);
            limit.setAmount(BigDecimal.valueOf(300));
            settings.setExpenseCategory(ExpenseCategory.FOOD);
            settings.setAmount(BigDecimal.valueOf(100));
            Wallet wallet = sufficientWallet();
            when(financialPeriodService.getExpensesSum(userId, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(BigDecimal.valueOf(100));

            assertDoesNotThrow(() -> expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of(limit)));
        }

        @Test
        void shouldPassWhenTotalEqualsLimit() {
            Limit limit = new Limit();
            limit.setCategory(ExpenseCategory.FOOD);
            limit.setPeriodType(PeriodType.MONTHLY);
            limit.setAmount(BigDecimal.valueOf(200));
            settings.setExpenseCategory(ExpenseCategory.FOOD);
            settings.setAmount(BigDecimal.valueOf(100));
            Wallet wallet = sufficientWallet();
            when(financialPeriodService.getExpensesSum(userId, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(BigDecimal.valueOf(100));

            assertDoesNotThrow(() -> expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of(limit)));
        }

        @Test
        void shouldThrowExceptionWhenSecondLimitInListExceeded() {
            Limit passingLimit = new Limit();
            passingLimit.setCategory(ExpenseCategory.TRANSPORT);
            passingLimit.setPeriodType(PeriodType.MONTHLY);
            passingLimit.setAmount(BigDecimal.valueOf(500));

            Limit failingLimit = new Limit();
            failingLimit.setCategory(ExpenseCategory.FOOD);
            failingLimit.setPeriodType(PeriodType.MONTHLY);
            failingLimit.setAmount(BigDecimal.valueOf(150));

            settings.setExpenseCategory(ExpenseCategory.FOOD);
            settings.setAmount(BigDecimal.valueOf(100));
            Wallet wallet = sufficientWallet();
            when(financialPeriodService.getExpensesSum(userId, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(BigDecimal.valueOf(100));

            assertThrows(InvalidInputException.class,
                    () -> expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of(passingLimit, failingLimit)));
        }

        @Test
        void shouldNotCallFinancialPeriodServiceWhenLimitsListIsEmpty() {
            Wallet wallet = sufficientWallet();

            expenseSettingsValidator.validate(settings, expenseSettings, wallet, List.of());

            verifyNoInteractions(financialPeriodService);
        }
    }
}