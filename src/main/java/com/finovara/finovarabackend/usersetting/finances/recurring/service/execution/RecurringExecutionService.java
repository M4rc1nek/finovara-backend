package com.finovara.finovarabackend.usersetting.finances.recurring.service.execution;

import com.finovara.finovarabackend.expense.dto.ExpenseDto;
import com.finovara.finovarabackend.expense.dto.ExpenseRequestDto;
import com.finovara.finovarabackend.expense.service.ExpenseService;
import com.finovara.finovarabackend.piggybank.service.PiggyBankTransactionService;
import com.finovara.finovarabackend.revenue.dto.RevenueDto;
import com.finovara.finovarabackend.revenue.service.RevenueService;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringDescription;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringType;
import com.finovara.finovarabackend.usersetting.finances.recurring.service.support.RecurringSettingsSupport;
import com.finovara.finovarabackend.usersetting.finances.recurring.service.validator.RecurringExpenseValidator;
import com.finovara.finovarabackend.usersetting.finances.recurring.service.validator.RecurringRevenueValidator;
import com.finovara.finovarabackend.usersetting.finances.recurring.service.validator.RecurringSavingsValidator;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.piggybank.exception.notfound.PiggyBankNotFoundException;
import jakarta.persistence.EntityNotFoundException;
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
        RevenueDto dto = buildRevenueDto(settings, date);
        recurringRevenueValidator.validate(settings);
        revenueService.addRevenue(dto, settings.getUserAssigned().getId());
    }

    private void createSavings(RecurringSettings settings) {
        if (settings.getUserAssigned() == null || settings.getPiggyBankId() == null) {
            return;
        }
        recurringSavingsValidator.validate(settings, settings.getUserAssigned().getWallet());
        try{
            piggyBankTransactionService.addBalanceToPiggyBank(settings.getUserAssigned().getId(), settings.getPiggyBankId(), settings.getAmount());
        }catch (PiggyBankNotFoundException e){
            log.warn("PiggyBank not found for recurring settings id={}, disabling", settings.getId());
            settings.setEnable(false);

        }
    }

    private void createExpense(RecurringSettings settings, LocalDate date) {
        if (settings.getUserAssigned() == null || settings.getExpenseCategory() == null) {
            return;
        }

        ExpenseSettings expenseSettings = settings.getUserAssigned().getExpenseSettings();
        if (expenseSettings == null) {
            return;

        }

        ExpenseDto expenseDto = buildExpenseDto(settings, date);

        var limitPeriodType = expenseSettings.getPeriodType() != null ? expenseSettings.getPeriodType() : settings.getPeriodType();

        CountQuantityLimitDto countQuantityLimitDto = new CountQuantityLimitDto(expenseSettings.isCountQuantityLimitEnabled(),
                limitPeriodType, expenseSettings.getNumberOfQuantityLimit());

        ExpenseRequestDto requestDto = new ExpenseRequestDto(expenseDto, new ConfirmPasswordDto(null), countQuantityLimitDto);
        recurringExpenseValidator.validate(settings, expenseSettings, settings.getUserAssigned().getWallet());
        expenseService.addExpense(requestDto, settings.getUserAssigned().getId(), limitPeriodType);
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