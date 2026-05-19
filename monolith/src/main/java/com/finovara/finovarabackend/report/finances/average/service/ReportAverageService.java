package com.finovara.finovarabackend.report.finances.average.service;

import com.finovara.finovarabackend.report.dto.ReportDto;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.periodbalance.FinancialPeriodService;
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
