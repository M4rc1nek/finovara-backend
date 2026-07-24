package com.finovara.reportservice.report.finances.calculate.highesttransactions.highestexpense.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HighestExpenseService {

    private final FinanceBackendReportClient reportClient;
    private final Clock clock;

    @Value("${expenses.highest.page-size}")
    private int pageSize;

    @Cacheable(value = "report:highestExpense", key = "#userId + ':' + #periodType")
    public List<HighestExpenseDto> getHighestExpense(Long userId, PeriodType periodType) {
        if (periodType == null) {
            throw new InvalidInputException("Unsupported report period type.");
        }
        LocalDate to = LocalDate.now(clock);
        LocalDate from = periodType.getStartDate(to);
        return reportClient.highestExpenses(userId, from, to, pageSize);
    }
}