package com.finovara.finovarabackend.report.finances.sum.sevice;

import com.finovara.finovarabackend.report.dto.ReportDto;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReportSumService {

    private final FinancialPeriodService financialPeriodService;

    public ReportDto sumExpense(Long userId, PeriodType periodType) {

        BigDecimal amount = switch (periodType) {
            case DAILY -> financialPeriodService.getSummedExpenseToday(userId);
            case WEEKLY -> financialPeriodService.getSummedExpenseWeekly(userId);
            case MONTHLY -> financialPeriodService.getSummedExpenseMonthly(userId);

        };

        return new ReportDto(periodType, amount);
    }

    public ReportDto sumRevenue(Long userId, PeriodType periodType){
        BigDecimal amount = switch (periodType){
            case DAILY -> financialPeriodService.getSummedRevenuesToday(userId);
            case WEEKLY -> financialPeriodService.getSummedRevenuesWeekly(userId);
            case MONTHLY -> financialPeriodService.getSummedRevenuesMonthly(userId);
        };
        return new ReportDto(periodType, amount);

    }

}
