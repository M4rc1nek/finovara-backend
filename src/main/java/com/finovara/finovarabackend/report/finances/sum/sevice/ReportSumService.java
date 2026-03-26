package com.finovara.finovarabackend.report.finances.sum.sevice;

import com.finovara.finovarabackend.report.finances.sum.dto.ReportSumDto;
import com.finovara.finovarabackend.report.finances.sum.model.ReportSumType;
import com.finovara.finovarabackend.util.service.time.SpentInPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReportSumService {

    private final SpentInPeriodService spentInPeriodService;

    public ReportSumDto sumRevenueAndExpense(Long userId, ReportSumType reportSumType) {

        BigDecimal amount = switch (reportSumType) {
            case DAILY -> spentInPeriodService.getSummedSpentToday(userId);
            case WEEKLY -> spentInPeriodService.getSummedSpentWeekly(userId);
            case MONTHLY -> spentInPeriodService.getSummedSpentMonthly(userId);

        };

        return new ReportSumDto(reportSumType, amount);
    }

}
