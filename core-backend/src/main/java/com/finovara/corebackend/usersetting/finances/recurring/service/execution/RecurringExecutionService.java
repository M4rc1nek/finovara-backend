package com.finovara.corebackend.usersetting.finances.recurring.service.execution;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.corebackend.expense.dto.ExpenseDto;
import com.finovara.corebackend.expense.dto.ExpenseRequestDto;
import com.finovara.corebackend.expense.service.ExpenseService;
import com.finovara.corebackend.piggybank.service.PiggyBankTransactionService;
import com.finovara.corebackend.revenue.dto.RevenueDto;
import com.finovara.corebackend.revenue.service.RevenueService;
import com.finovara.corebackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.corebackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.corebackend.usersetting.finances.recurring.model.RecurringDescription;
import com.finovara.corebackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.corebackend.usersetting.finances.recurring.service.validator.RecurringExpenseValidator;
import com.finovara.corebackend.usersetting.finances.recurring.service.validator.RecurringRevenueValidator;
import com.finovara.corebackend.usersetting.finances.recurring.service.validator.RecurringSavingsValidator;
import com.finovara.contracts.dto.ConfirmPasswordDto;
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
        if(settings.getUserAssigned() == null || settings.getRevenueCategory() == null){
            return;
        }
        recurringRevenueValidator.validate(settings);
        RevenueDto dto = buildRevenueDto(settings, date);
        revenueService.addRevenue(dto, settings.getUserAssigned().getId());
    }

    private void createExpense(RecurringSettings settings, LocalDate date) {
        if (settings.getUserAssigned() == null || settings.getExpenseCategory() == null) {
            return;
        }

        ExpenseSettings expenseSettings = settings.getUserAssigned().getExpenseSettings();
        if (expenseSettings == null) {
            return;
        }

        recurringExpenseValidator.validate(settings, expenseSettings, settings.getUserAssigned().getWallet());

        PeriodType limitPeriodType = resolveLimitPeriodType(settings, expenseSettings);
        ExpenseDto expenseDto = buildExpenseDto(settings, date);

        ExpenseRequestDto requestDto = new ExpenseRequestDto(expenseDto, new ConfirmPasswordDto(null), buildCountQuantityLimitDto(expenseSettings, limitPeriodType));

        expenseService.addExpense(requestDto, settings.getUserAssigned().getId(), limitPeriodType);
    }

    private void createSavings(RecurringSettings settings) {
        if (settings.getUserAssigned() == null || settings.getPiggyBankId() == null) {
            return;
        }
        recurringSavingsValidator.validate(settings, settings.getUserAssigned().getWallet());

        try {
            piggyBankTransactionService.addBalanceToPiggyBank(settings.getUserAssigned().getId(), settings.getPiggyBankId(), settings.getAmount(),
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
                settings.getUserAssigned().getId(),
                settings.getAmount(),
                settings.getExpenseCategory(),
                date,
                RecurringDescription.EXPENSE.label()
        );
    }

    private RevenueDto buildRevenueDto(RecurringSettings settings, LocalDate date) {
        return new RevenueDto(
                null,
                settings.getUserAssigned().getId(),
                settings.getAmount(),
                settings.getRevenueCategory(),
                date,
                RecurringDescription.REVENUE.label()
        );
    }
}
