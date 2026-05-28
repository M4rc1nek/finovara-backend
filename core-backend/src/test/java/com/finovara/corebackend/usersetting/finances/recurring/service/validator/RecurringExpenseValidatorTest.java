package com.finovara.corebackend.usersetting.finances.recurring.service.validator;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.corebackend.usersetting.finances.expense.smartscan.exception.conflict.SmartScanConfirmationRequiredException;
import com.finovara.corebackend.usersetting.finances.expense.smartscan.service.SmartScanService;
import com.finovara.corebackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.corebackend.usersetting.finances.recurring.service.validator.util.RecurringBasicValidator;
import com.finovara.contracts.model.PeriodType;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.wallet.model.Wallet;
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
class RecurringExpenseValidatorTest {

    @Mock
    private SmartScanService smartScanService;

    @Mock
    private RecurringBasicValidator recurringBasicValidator;

    @InjectMocks
    private RecurringExpenseValidator recurringExpenseValidator;

    private RecurringSettings recurringSettings;
    private ExpenseSettings expenseSettings;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        recurringSettings = new RecurringSettings();
        recurringSettings.setAmount(BigDecimal.valueOf(100));
        recurringSettings.setStartDate(LocalDate.of(2025, 1, 1));
        recurringSettings.setPeriodType(PeriodType.MONTHLY);
        recurringSettings.setNextExecutionDate(LocalDate.of(2025, 1, 1));

        expenseSettings = new ExpenseSettings();
        wallet = Wallet.create(new User());

        recurringSettings.setUserAssigned(new User());
        recurringSettings.getUserAssigned().setId(1L);
    }

    @Nested
    class Validate {

        @Test
        void shouldPassValidationWhenAllRulesAreValid() {
            recurringSettings.setExpenseCategory(ExpenseCategory.FOOD);

            wallet.deposit(BigDecimal.valueOf(1000));

            expenseSettings.setCountQuantityLimitEnabled(false);
            expenseSettings.setAmountThresholdEnabled(false);
            expenseSettings.setSmartScanEnabled(false);

            recurringExpenseValidator.validate(recurringSettings, expenseSettings, wallet);

            verify(recurringBasicValidator).validateBasics(recurringSettings, recurringSettings.getExpenseCategory());
        }

        @Test
        void shouldThrowExceptionWhenBalanceIsInsufficient() {
            recurringSettings.setExpenseCategory(ExpenseCategory.FOOD);

            wallet.deposit(BigDecimal.valueOf(10));

            expenseSettings.setCountQuantityLimitEnabled(false);
            expenseSettings.setAmountThresholdEnabled(false);
            expenseSettings.setSmartScanEnabled(false);

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringExpenseValidator.validate(recurringSettings, expenseSettings, wallet));

            assertEquals("Insufficient funds", invalidInputException.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenAmountThresholdExceeded() {
            recurringSettings.setExpenseCategory(ExpenseCategory.FOOD);

            wallet.deposit(BigDecimal.valueOf(1000));

            expenseSettings.setAmountThresholdEnabled(true);
            expenseSettings.setBlockedAmount(BigDecimal.valueOf(50));

            expenseSettings.setCountQuantityLimitEnabled(false);
            expenseSettings.setSmartScanEnabled(false);

            recurringSettings.setAmount(BigDecimal.valueOf(100));

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringExpenseValidator.validate(recurringSettings, expenseSettings, wallet));

            assertEquals("Expense amount exceeds the allowed limit: 50", invalidInputException.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenSmartScanFails() {
            recurringSettings.setExpenseCategory(ExpenseCategory.FOOD);

            wallet.deposit(BigDecimal.valueOf(1000));

            expenseSettings.setSmartScanEnabled(true);
            expenseSettings.setCountQuantityLimitEnabled(false);
            expenseSettings.setAmountThresholdEnabled(false);

            doThrow(new SmartScanConfirmationRequiredException("smart scan"))
                    .when(smartScanService)
                    .handleSmartScan(anyLong(), any(), any(), any());

            InvalidInputException invalidInputException = assertThrows(
                    InvalidInputException.class,
                    () -> recurringExpenseValidator.validate(recurringSettings, expenseSettings, wallet)
            );

            assertEquals("You cannot create this recurring expense because the amount is considered unusual. Try lowering the amount or disable Smart Scan.",
                    invalidInputException.getMessage()
            );
        }

        @Test
        void shouldThrowExceptionWhenQuantityLimitExceeded() {
            recurringSettings.setExpenseCategory(ExpenseCategory.FOOD);

            wallet.deposit(BigDecimal.valueOf(1000));

            expenseSettings.setCountQuantityLimitEnabled(true);
            expenseSettings.setNumberOfQuantityLimit(0);

            expenseSettings.setAmountThresholdEnabled(false);
            expenseSettings.setSmartScanEnabled(false);

            recurringSettings.setNextExecutionDate(LocalDate.now());

            InvalidInputException invalidInputException = assertThrows(InvalidInputException.class,
                    () -> recurringExpenseValidator.validate(recurringSettings, expenseSettings, wallet));

            assertTrue(invalidInputException.getMessage().contains("Expense count limit exceeded"));
        }
    }
}