package com.finovara.finovarabackend.usersetting.finances.recurring.service;

import com.finovara.finovarabackend.expense.service.ExpenseService;
import com.finovara.finovarabackend.revenue.dto.RevenueDto;
import com.finovara.finovarabackend.revenue.service.RevenueService;
import com.finovara.finovarabackend.usersetting.finances.recurring.model.RecurringSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RecurringExecutionService {

    private final RevenueService revenueService;
    private final ExpenseService expenseService;

    public void execute(RecurringSettings settings, LocalDate date) {
        if (settings.getType() == null) return;

        switch (settings.getType()) {
            case REVENUE -> createRevenue(settings, date);
            case EXPENSE -> createExpense(settings, date);
            case SAVINGS -> createSavings(settings, date);
        }
    }

    private void createRevenue(RecurringSettings settings, LocalDate date) {
        RevenueDto dto = new RevenueDto(
                null,
                settings.getUserAssigned().getId(),
                settings.getAmount(),
                settings.getRevenueCategory(),
                date,
                "Recurring revenue"
        );

        revenueService.addRevenue(dto, settings.getUserAssigned().getId());
    }

    private void createExpense(RecurringSettings settings, LocalDate date) {
        //do zrobienia
    }

    private void createSavings(RecurringSettings settings, LocalDate date) {
        // savings logic
    }
}