package com.finovara.corebackend.report.finances.highesttransactions.highestexpense.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.expense.repository.ExpenseRepository;
import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.contracts.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HighestExpenseService {
    private final ExpenseRepository expenseRepository;

    @Value("${expenses.highest.page-size}")
    private int pageSize;

    public List<HighestExpenseDto> getHighestExpense(Long userId, PeriodType periodType) {
        if (periodType == null) {
            throw new InvalidInputException("Unsupported report period type.");
        }
        LocalDate today = LocalDate.now();
        LocalDate from = periodType.getStartDate(today);

        return expenseRepository.findHighestExpensesByUserAssignedIdAndPeriod(userId, from, today, PageRequest.of(0, pageSize));

    }

}
