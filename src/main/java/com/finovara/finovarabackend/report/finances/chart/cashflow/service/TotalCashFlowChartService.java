package com.finovara.finovarabackend.report.finances.chart.cashflow.service;

import com.finovara.finovarabackend.report.finances.chart.cashflow.dto.TotalCashFlowDto;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TotalCashFlowChartService {
    private final UserManagerService userManagerService;
    private final FinancialPeriodService financialPeriodService;

    public List<TotalCashFlowDto> getCashFlowChart(Long userId, PeriodType periodType) {
        LocalDate today = LocalDate.now();
        LocalDate start = financialPeriodService.getStartDate(today, periodType);
        List<TotalCashFlowDto> chartData = new ArrayList<>();

        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            BigDecimal spentInPeriod = financialPeriodService.calculateExpenseInPeriod(userId, date, date);
            BigDecimal earnedInPeriod = financialPeriodService.calculateRevenueInPeriod(userId, date, date);

            chartData.add(new TotalCashFlowDto(date, spentInPeriod, earnedInPeriod));
        }

        return chartData;
    }
}
