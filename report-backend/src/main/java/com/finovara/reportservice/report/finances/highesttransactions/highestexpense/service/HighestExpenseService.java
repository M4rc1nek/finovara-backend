package com.finovara.reportservice.report.finances.highesttransactions.highestexpense.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.contracts.model.PeriodType;
import com.finovara.reportservice.feignclient.CoreBackendReportClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HighestExpenseService {

    private final CoreBackendReportClient reportClient;

    @Value("${expenses.highest.page-size}")
    private int pageSize;

    public List<HighestExpenseDto> getHighestExpense(Long userId, PeriodType periodType) {
        if (periodType == null) {
            throw new InvalidInputException("Unsupported report period type.");
        }
        LocalDate to = LocalDate.now();
        LocalDate from = periodType.getStartDate(to);
        return reportClient.highestExpenses(userId, from, to, pageSize);
    }
}

