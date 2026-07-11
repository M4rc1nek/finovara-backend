package com.finovara.reportservice.sharedaccount.report.finances.highesttransactions.highestexpense.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.reportservice.feignclient.FinanceBackendSharedReportClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SharedHighestExpenseService {

    private final FinanceBackendSharedReportClient reportClient;
    private final Clock clock;

    @Value("${shared.expenses.highest.page-size}")
    private int pageSize;

    @Cacheable(value = "report:sharedHighestExpense", key = "#userId + ':' + #periodType")
    public List<HighestExpenseDto> getHighestExpense(Long userId, PeriodType periodType) {
        if (periodType == null) {
            throw new InvalidInputException("Unsupported report period type.");
        }
        LocalDate to = LocalDate.now(clock);
        LocalDate from = periodType.getStartDate(to);
        return reportClient.highestExpenses(userId, from, to, pageSize);
    }
}