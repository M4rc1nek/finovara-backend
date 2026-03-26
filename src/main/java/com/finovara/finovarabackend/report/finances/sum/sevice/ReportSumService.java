package com.finovara.finovarabackend.report.finances.sum.sevice;

import com.finovara.finovarabackend.report.finances.sum.dto.ReportSumDto;
import com.finovara.finovarabackend.report.finances.sum.model.ReportSumType;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReportSumService {

    private final FinancialPeriodService financialPeriodService;

    public ReportSumDto sumExpense(Long userId, ReportSumType reportSumType) {

        BigDecimal amount = switch (reportSumType) {
            case DAILY -> financialPeriodService.getSummedExpenseToday(userId);
            case WEEKLY -> financialPeriodService.getSummedExpenseWeekly(userId);
            case MONTHLY -> financialPeriodService.getSummedExpenseMonthly(userId);

        };

        return new ReportSumDto(reportSumType, amount);
    }

    public ReportSumDto sumRevenue(Long userId, ReportSumType reportSumType){
        BigDecimal amount = switch (reportSumType){
            case DAILY -> financialPeriodService.getSummedRevenuesToday(userId);
            case WEEKLY -> financialPeriodService.getSummedRevenuesWeekly(userId);
            case MONTHLY -> financialPeriodService.getSummedRevenuesMonthly(userId);
        };
        return new ReportSumDto(reportSumType, amount);

    }

}
