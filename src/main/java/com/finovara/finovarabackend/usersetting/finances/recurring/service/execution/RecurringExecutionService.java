package com.finovara.finovarabackend.usersetting.finances.recurring.service;

import com.finovara.finovarabackend.expense.dto.ExpenseDto;
import com.finovara.finovarabackend.expense.dto.ExpenseRequestDto;
import com.finovara.finovarabackend.expense.service.ExpenseService;
import com.finovara.finovarabackend.piggybank.service.PiggyBankTransactionService;
import com.finovara.finovarabackend.revenue.dto.RevenueDto;
import com.finovara.finovarabackend.revenue.service.RevenueService;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.dto.CountQuantityLimitDto;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RecurringExecutionService {

    private final RevenueService revenueService;
    private final ExpenseService expenseService;
    private final PiggyBankTransactionService piggyBankTransactionService;

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
        RevenueDto dto = new RevenueDto(
                null,
                settings.getUserAssigned().getId(),
                settings.getAmount(),
                settings.getRevenueCategory(),
                date,
                "Cykliczne przychody"
        );

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

        ExpenseDto expenseDto = new ExpenseDto(
                null,
                settings.getUserAssigned().getId(),
                settings.getAmount(),
                settings.getExpenseCategory(),
                date,
                "Cykliczne wydatki"
        );

        var limitPeriodType = expenseSettings.getPeriodType() != null ? expenseSettings.getPeriodType() : settings.getPeriodType();

        CountQuantityLimitDto countQuantityLimitDto = new CountQuantityLimitDto(expenseSettings.isCountQuantityLimitEnabled(),
                limitPeriodType, expenseSettings.getNumberOfQuantityLimit());

        // confirmPasswordDto zostaje puste, wiec limit awaryjny wymaga recznej akcji uzytkownika
        ExpenseRequestDto requestDto = new ExpenseRequestDto(expenseDto, new ConfirmPasswordDto(null), countQuantityLimitDto);
        expenseService.addExpense(requestDto, settings.getUserAssigned().getId(), limitPeriodType);
    }

    private void createSavings(RecurringSettings settings) {
        if (settings.getUserAssigned() == null || settings.getPiggyBankId() == null) {
            return;
        }
        piggyBankTransactionService.addBalanceToPiggyBank(settings.getUserAssigned().getId(), settings.getPiggyBankId(), settings.getAmount());
    }
}