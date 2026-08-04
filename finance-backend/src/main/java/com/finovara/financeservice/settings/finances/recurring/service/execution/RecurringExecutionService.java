package com.finovara.financeservice.settings.finances.recurring.service.execution;

import com.finovara.contracts.auth.dto.ConfirmAuthorizationCodeDto;
import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.financeservice.expense.dto.ExpenseDto;
import com.finovara.financeservice.expense.dto.ExpenseRequestDto;
import com.finovara.financeservice.expense.service.ExpenseService;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.piggybank.service.PiggyBankTransactionService;
import com.finovara.financeservice.revenue.dto.RevenueDto;
import com.finovara.financeservice.revenue.service.RevenueService;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.quantitylimit.dto.CountQuantityLimitDto;
import com.finovara.financeservice.settings.finances.expense.repository.ExpenseSettingsRepository;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringDescription;
import com.finovara.financeservice.settings.finances.recurring.model.RecurringSettings;
import com.finovara.financeservice.settings.finances.recurring.service.validator.ExpenseSettingsValidator;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringRevenueValidator;
import com.finovara.financeservice.settings.finances.recurring.service.validator.RecurringSavingsValidator;
import com.finovara.financeservice.util.limit.manager.LimitManagerService;
import com.finovara.financeservice.util.wallet.WalletManagerService;
import com.finovara.financeservice.wallet.model.Wallet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringExecutionService {

    private final RevenueService revenueService;
    private final ExpenseService expenseService;
    private final PiggyBankTransactionService piggyBankTransactionService;
    private final ExpenseSettingsValidator expenseSettingsValidator;
    private final RecurringRevenueValidator recurringRevenueValidator;
    private final RecurringSavingsValidator recurringSavingsValidator;
    private final ExpenseSettingsRepository expenseSettingsRepository;
    private final WalletManagerService walletManagerService;
    private final LimitManagerService limitManagerService;

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
        List<Limit> limits = limitManagerService.getLimitsByUserId(settings.getUserId());
        expenseSettingsValidator.validate(settings, expenseSettings, wallet, limits);

        PeriodType limitPeriodType = resolveLimitPeriodType(settings, expenseSettings);
        ExpenseDto expenseDto = buildExpenseDto(settings, date);

        ExpenseRequestDto requestDto = new ExpenseRequestDto(expenseDto, new ConfirmPasswordDto(null), new ConfirmAuthorizationCodeDto(null), buildCountQuantityLimitDto(expenseSettings, limitPeriodType));

        expenseService.addExpense(requestDto, settings.getUserId());
    }

    private void createSavings(RecurringSettings settings) {
        if (settings.getUserId() == null || settings.getPiggyBankId() == null) {
            return;
        }
        Wallet wallet = walletManagerService.getWalletByUserIdOrThrow(settings.getUserId());
        recurringSavingsValidator.validate(settings, wallet);

        try {
            piggyBankTransactionService.addBalanceToPiggyBank(settings.getUserId(), settings.getPiggyBankId(), settings.getAmount(),
                    PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_BY_SETTING, null);
        } catch (RequestedEntityNotFoundException e) {
            log.warn("SharedPiggyBank not found for recurring settings id={}, disabling", settings.getId());
            settings.setEnable(false);
            settings.setNextExecutionDate(null);
        }

    }

    private PeriodType resolveLimitPeriodType(RecurringSettings settings, ExpenseSettings expenseSettings) {
        return expenseSettings.getPeriodType() != null ? expenseSettings.getPeriodType() : settings.getPeriodType();
    }

    private CountQuantityLimitDto buildCountQuantityLimitDto(ExpenseSettings expenseSettings, PeriodType limitPeriodType) {
        return new CountQuantityLimitDto(expenseSettings.isCountQuantityLimitEnabled(), limitPeriodType, expenseSettings.getNumberOfQuantityLimit(), null);
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
                RecurringDescription.REVENUE.label(),
                null
        );
    }
}
