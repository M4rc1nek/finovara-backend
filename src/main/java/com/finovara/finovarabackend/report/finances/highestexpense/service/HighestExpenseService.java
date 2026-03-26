package com.finovara.finovarabackend.report.finances.highestexpense.service;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.finances.highestexpense.dto.ReportsHighestExpense;
import com.finovara.finovarabackend.report.model.ReportPeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HighestExpenseService {
    private final ExpenseRepository expenseRepository;

    public List<ReportsHighestExpense> getHighestExpense(Long userId, ReportPeriodType reportPeriodType) {
        if (reportPeriodType == null) {
            throw new InvalidInputException("Unsupported report period type.");
        }
        LocalDate today = LocalDate.now();
        LocalDate from;
        switch (reportPeriodType) {
            case DAILY -> from = today;
            case WEEKLY -> from = today.with(DayOfWeek.MONDAY);
            case MONTHLY -> from = today.withDayOfMonth(1);
            default -> throw new IllegalStateException("Unexpected value: " + reportPeriodType);
        }

        return expenseRepository.findHighestExpensesByUserAssignedIdAndPeriod(userId, from, today, PageRequest.of(0, 5));

    }

}
