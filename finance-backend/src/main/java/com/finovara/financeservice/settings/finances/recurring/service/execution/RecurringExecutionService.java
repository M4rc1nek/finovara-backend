package com.finovara.financeservice.settings.finances.recurring.service.execution;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.mainaccount.activity.model.PiggyBankActivityType;
import com.finovara.contracts.user.authorization.dto.ConfirmAuthorizationCodeDto;
import com.finovara.contracts.user.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.financeservice.expense.dto.ExpenseDto;
import com.finovara.financeservice.expense.dto.ExpenseRequestDto;
import com.finovara.financeservice.expense.service.ExpenseService;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import com.finovara.financeservice.piggybank.service.PiggyBankTransactionService;
import com.finovara.financeservice.revenue.dto.RevenueDto;
import com.finovara.financeservice.revenue.service.RevenueService;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.quantitylimit.dto.CountQuantityLimitDto;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringDescription;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.service.validator.ExpenseSettingsValidator;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringRevenueValidator;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringSavingsValidator;
import com.finovara.financeservice.util.limit.manager.LimitManagerService;
import com.finovara.financeservice.util.settings.SettingsManager;
import com.finovara.financeservice.util.transaction.TransactionOrigin;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringExecutionService {

    private final SettingsManager settingsManager;
    private final RevenueService revenueService;
    private final ExpenseService expenseService;
    private final PiggyBankTransactionService piggyBankTransactionService;
    private final PiggyBankRepository piggyBankRepository;
    private final ExpenseSettingsValidator expenseSettingsValidator;
    private final RecurringRevenueValidator recurringRevenueValidator;
    private final RecurringSavingsValidator recurringSavingsValidator;
    private final WalletService walletService;
    private final WalletManagerService walletManagerService;
    private final LimitManagerService limitManagerService;

    public RecurringExecutionContext prepareContext(RecurringSettings settings) {
        Long userId = settings.getUserId();

        walletService.lockWalletForUpdate(userId);
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(userId);

        return switch (settings.getType()) {
            case EXPENSE -> new RecurringExecutionContext(wallet, settingsManager.getExpenseSettingsByUserId(userId),
                    limitManagerService.getLimitsByUserId(userId), false);
            case SAVINGS -> new RecurringExecutionContext(wallet, null, List.of(), piggyBankExists(settings));
            case REVENUE -> new RecurringExecutionContext(wallet, null, List.of(), false);
        };
    }

    public RecurringExecutionResult execute(RecurringSettings settings, LocalDate date, RecurringExecutionContext context) {
        if (settings.getType() == null) {
            return RecurringExecutionResult.EXECUTED;
        }

        return switch (settings.getType()) {
            case REVENUE -> createRevenue(settings, date);
            case EXPENSE -> createExpense(settings, date, context);
            case SAVINGS -> createSavings(settings, context);
        };
    }

    private RecurringExecutionResult createRevenue(RecurringSettings settings, LocalDate date) {
        if (settings.getUserId() == null || settings.getRevenueCategory() == null) {
            return RecurringExecutionResult.EXECUTED;
        }

        try {
            recurringRevenueValidator.validate(settings);

            RevenueDto revenueDto = buildRevenueDto(settings, date);
            revenueService.addRevenue(revenueDto, settings.getUserId(), null, TransactionOrigin.RECURRING_SYSTEM);

            return RecurringExecutionResult.EXECUTED;

        } catch (InvalidInputException exception) {
            log.warn("Recurring revenue skipped for settings id={}: {}", settings.getId(), exception.getMessage());

            return RecurringExecutionResult.SKIPPED;
        }
    }

    private RecurringExecutionResult createExpense(RecurringSettings settings, LocalDate date, RecurringExecutionContext context) {
        if (settings.getUserId() == null || settings.getExpenseCategory() == null) {
            return RecurringExecutionResult.EXECUTED;
        }

        ExpenseSettings expenseSettings = context.expenseSettings();

        if (expenseSettings == null) {
            return RecurringExecutionResult.EXECUTED;
        }

        try {
            expenseSettingsValidator.validate(settings, expenseSettings, context.wallet(), context.limits());
            ExpenseRequestDto requestDto = buildExpenseRequest(settings, expenseSettings, date);
            expenseService.addExpense(requestDto, settings.getUserId(), null, TransactionOrigin.RECURRING_SYSTEM);

            return RecurringExecutionResult.EXECUTED;

        } catch (InvalidInputException exception) {
            log.warn("Recurring expense skipped for settings id={}: {}", settings.getId(), exception.getMessage());
            return RecurringExecutionResult.SKIPPED;
        }
    }

    private RecurringExecutionResult createSavings(RecurringSettings settings, RecurringExecutionContext context) {
        if (settings.getUserId() == null || settings.getPiggyBankId() == null) {
            return RecurringExecutionResult.EXECUTED;
        }

        if (!context.piggyBankExists()) {
            log.warn("PiggyBank not found for recurring settings id={}, disabling", settings.getId());

            disableRecurringSettings(settings);
            return RecurringExecutionResult.EXECUTED;
        }

        try {
            recurringSavingsValidator.validate(settings, context.wallet());
            executeSavings(settings);
            return RecurringExecutionResult.EXECUTED;

        } catch (InvalidInputException exception) {
            log.warn("Recurring savings skipped for settings id={}: {}", settings.getId(), exception.getMessage());
            return RecurringExecutionResult.SKIPPED;
        }
    }

    private boolean piggyBankExists(RecurringSettings settings) {
        return settings.getPiggyBankId() != null
                && piggyBankRepository.findByIdAndUserId(settings.getPiggyBankId(), settings.getUserId()).isPresent();
    }

    private void executeSavings(RecurringSettings settings) {
        piggyBankTransactionService.addBalanceToPiggyBank(settings.getUserId(), settings.getPiggyBankId(), settings.getAmount(), PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, null, null, null, TransactionOrigin.RECURRING_SYSTEM);
    }

    private void disableRecurringSettings(RecurringSettings settings) {
        settings.setEnable(false);
        settings.setNextExecutionDate(null);
    }

    private ExpenseRequestDto buildExpenseRequest(RecurringSettings settings, ExpenseSettings expenseSettings, LocalDate date) {
        PeriodType limitPeriodType = getLimitPeriodType(settings, expenseSettings);

        return new ExpenseRequestDto(buildExpenseDto(settings, date), new ConfirmPasswordDto(null), new ConfirmAuthorizationCodeDto(null), new CountQuantityLimitDto(expenseSettings.isCountQuantityLimitEnabled(), limitPeriodType, expenseSettings.getNumberOfQuantityLimit(), null), null);
    }

    private PeriodType getLimitPeriodType(RecurringSettings settings, ExpenseSettings expenseSettings) {
        return expenseSettings.getPeriodType() != null ? expenseSettings.getPeriodType() : settings.getPeriodType();
    }

    private ExpenseDto buildExpenseDto(RecurringSettings settings, LocalDate date) {
        return new ExpenseDto(null, settings.getUserId(), settings.getAmount(), settings.getExpenseCategory(), date, RecurringDescription.EXPENSE.label());
    }

    private RevenueDto buildRevenueDto(RecurringSettings settings, LocalDate date) {
        return new RevenueDto(null, settings.getUserId(), settings.getAmount(), settings.getRevenueCategory(), date, RecurringDescription.REVENUE.label(), null, null);
    }
}