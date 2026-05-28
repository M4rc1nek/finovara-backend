package com.finovara.corebackend.report.finances.average.service;

import com.finovara.corebackend.report.dto.ReportDto;
import com.finovara.contracts.model.PeriodType;
import com.finovara.corebackend.util.periodbalance.FinancialPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReportAverageService {

    private final FinancialPeriodService financialPeriodService;

    public ReportDto calculateAverageExpense(Long userId, PeriodType periodType) {
        BigDecimal amount = financialPeriodService.getAverageExpense(userId, periodType);
        return new ReportDto(periodType, amount);
    }

    public ReportDto calculateAverageRevenue(Long userId, PeriodType periodType) {
        BigDecimal amount = financialPeriodService.getAverageRevenue(userId, periodType);
        return new ReportDto(periodType, amount);
    }

}
