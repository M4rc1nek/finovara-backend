package com.finovara.reportservice.report.finances.sum.service;

import com.finovara.reportservice.feignclient.CoreBackendReportClient;
import com.finovara.reportservice.report.dto.ReportDto;
import com.finovara.contracts.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ReportSummaryService {

    private final CoreBackendReportClient reportClient;

    public ReportDto sumExpense(Long userId, PeriodType periodType) {
        LocalDate to = LocalDate.now();
        LocalDate from = periodType.getStartDate(to);
        BigDecimal amount = reportClient.sumExpenses(userId, from, to);
        return new ReportDto(periodType, amount);
    }

    public ReportDto sumRevenue(Long userId, PeriodType periodType) {
        LocalDate to = LocalDate.now();
        LocalDate from = periodType.getStartDate(to);
        BigDecimal amount = reportClient.sumRevenues(userId, from, to);
        return new ReportDto(periodType, amount);
    }
}