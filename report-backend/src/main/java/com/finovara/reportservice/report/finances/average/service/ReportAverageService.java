package com.finovara.reportservice.report.finances.average.service;

import com.finovara.reportservice.feignclient.CoreBackendReportClient;
import com.finovara.reportservice.report.dto.ReportDto;
import com.finovara.contracts.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ReportAverageService {

    private final CoreBackendReportClient reportClient;

    public ReportDto calculateAverageExpense(Long userId, PeriodType periodType) {
        LocalDate to = LocalDate.now();
        LocalDate from = periodType.getStartDate(to);
        BigDecimal amount = reportClient.avgExpenses(userId, from, to);
        return new ReportDto(periodType, amount);
    }

    public ReportDto calculateAverageRevenue(Long userId, PeriodType periodType) {
        LocalDate to = LocalDate.now();
        LocalDate from = periodType.getStartDate(to);
        BigDecimal amount = reportClient.avgRevenues(userId, from, to);
        return new ReportDto(periodType, amount);
    }
}
