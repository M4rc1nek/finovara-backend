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
            case DAILY -> financialPeriodService.getSpent(userId, PeriodType.DAILY);
            case WEEKLY -> financialPeriodService.getSpent(userId, PeriodType.WEEKLY);
            case MONTHLY -> financialPeriodService.getSpent(userId, PeriodType.MONTHLY);

        };

        return new ReportDto(periodType, amount);
    }

    public ReportDto sumRevenue(Long userId, PeriodType periodType){
        BigDecimal amount = switch (periodType){
            case DAILY -> financialPeriodService.getEarned(userId, PeriodType.DAILY);
            case WEEKLY -> financialPeriodService.getEarned(userId, PeriodType.WEEKLY);
            case MONTHLY -> financialPeriodService.getEarned(userId, PeriodType.MONTHLY);
        };
        return new ReportDto(periodType, amount);

    }

}
