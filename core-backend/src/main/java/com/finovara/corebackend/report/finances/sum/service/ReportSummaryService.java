package com.finovara.corebackend.report.finances.sum.service;

import com.finovara.corebackend.report.dto.ReportDto;
import com.finovara.contracts.model.PeriodType;
import com.finovara.corebackend.util.periodbalance.FinancialPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReportSummaryService {

    private final FinancialPeriodService financialPeriodService;

    public ReportDto sumExpense(Long userId, PeriodType periodType) {
        BigDecimal amount = financialPeriodService.getExpensesSum(userId, periodType);
        return new ReportDto(periodType, amount);
    }

    public ReportDto sumRevenue(Long userId, PeriodType periodType) {
        BigDecimal amount = financialPeriodService.getRevenueSum(userId, periodType);
        return new ReportDto(periodType, amount);
    }
}
