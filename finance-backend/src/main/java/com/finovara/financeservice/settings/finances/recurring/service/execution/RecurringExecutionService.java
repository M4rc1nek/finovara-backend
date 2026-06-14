package com.finovara.authbackend.usersetting.finances.recurring.service.execution;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.authbackend.expense.dto.ExpenseDto;
import com.finovara.authbackend.expense.dto.ExpenseRequestDto;
import com.finovara.authbackend.expense.service.ExpenseService;
import com.finovara.authbackend.piggybank.service.PiggyBankTransactionService;
import com.finovara.authbackend.revenue.dto.RevenueDto;
import com.finovara.authbackend.revenue.service.RevenueService;
import com.finovara.authbackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.authbackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.authbackend.usersetting.finances.expense.repository.ExpenseSettingsRepository;
import com.finovara.authbackend.usersetting.finances.recurring.model.RecurringDescription;
import com.finovara.authbackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.authbackend.usersetting.finances.recurring.service.validator.RecurringExpenseValidator;
import com.finovara.authbackend.usersetting.finances.recurring.service.validator.RecurringRevenueValidator;
import com.finovara.authbackend.usersetting.finances.recurring.service.validator.RecurringSavingsValidator;
import com.finovara.authbackend.util.wallet.WalletManagerService;
import com.finovara.authbackend.wallet.model.Wallet;
import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.contracts.model.PeriodType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringExecutionService {

    private final RevenueService revenueService;
    private final ExpenseService expenseService;
    private final PiggyBankTransactionService piggyBankTransactionService;
    private final RecurringExpenseValidator recurringExpenseValidator;
    private final RecurringRevenueValidator recurringRevenueValidator;
    private final RecurringSavingsValidator recurringSavingsValidator;
    private final ExpenseSettingsRepository expenseSettingsRepository;
    private final WalletManagerService walletManagerService;

    public void execute(RecurringSettings settings, LocalDate date) {
        if (settings.getType() == null) {
            return;
        }

        switch (settings.getType()) {
            case REVENUE -> createRevenue(settings, date);
            case EXPENSE -> createExpense(settings, date);
            case SAVINGS -> createSavings(settings);
        }
    }

    private void createRevenue(RecurringSettings settings, LocalDate date) {
        if (settings.getUserId() == null || settings.getRevenueCategory() == null) {
            return;
        }
        recurringRevenueValidator.validate(settings);
        RevenueDto dto = buildRevenueDto(settings, date);
        revenueService.addRevenue(dto, settings.getUserId());
    }

    private void createExpense(RecurringSettings settings, LocalDate date) {
        if (settings.getUserId() == null || settings.getExpenseCategory() == null) {
            return;
        }

        ExpenseSettings expenseSettings = expenseSettingsRepository.findByUserId(settings.getUserId());
        if (expenseSettings == null) {
            return;
        }

        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(settings.getUserId());
        recurringExpenseValidator.validate(settings, expenseSettings, wallet);

        PeriodType limitPeriodType = resolveLimitPeriodType(settings, expenseSettings);
        ExpenseDto expenseDto = buildExpenseDto(settings, date);

        ExpenseRequestDto requestDto = new ExpenseRequestDto(expenseDto, new ConfirmPasswordDto(null), buildCountQuantityLimitDto(expenseSettings, limitPeriodType));

        expenseService.addExpense(requestDto, settings.getUserId(), limitPeriodType);
    }

    private void createSavings(RecurringSettings settings) {
        if (settings.getUserId() == null || settings.getPiggyBankId() == null) {
            return;
        }
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(settings.getUserId());
        recurringSavingsValidator.validate(settings, wallet);

        try {
            piggyBankTransactionService.addBalanceToPiggyBank(settings.getUserId(), settings.getPiggyBankId(), settings.getAmount(),
                    PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING);
        } catch (RequestedEntityNotFoundException e) {
            log.warn("PiggyBank not found for recurring settings id={}, disabling", settings.getId());
            settings.setEnable(false);
            settings.setNextExecutionDate(null);
        }

    }

    private PeriodType resolveLimitPeriodType(RecurringSettings settings, ExpenseSettings expenseSettings) {
        return expenseSettings.getPeriodType() != null ? expenseSettings.getPeriodType() : settings.getPeriodType();
    }

    private CountQuantityLimitDto buildCountQuantityLimitDto(ExpenseSettings expenseSettings, PeriodType limitPeriodType) {
        return new CountQuantityLimitDto(expenseSettings.isCountQuantityLimitEnabled(), limitPeriodType, expenseSettings.getNumberOfQuantityLimit());
    }

    private ExpenseDto buildExpenseDto(RecurringSettings settings, LocalDate date) {
        return new ExpenseDto(
                null,
                settings.getUserId(),
                settings.getAmount(),
                settings.getExpenseCategory(),
                date,
                RecurringDescription.EXPENSE.label()
        );
    }

    private RevenueDto buildRevenueDto(RecurringSettings settings, LocalDate date) {
        return new RevenueDto(
                null,
                settings.getUserId(),
                settings.getAmount(),
                settings.getRevenueCategory(),
                date,
                RecurringDescription.REVENUE.label()
        );
    }
}
