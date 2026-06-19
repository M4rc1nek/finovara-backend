package com.finovara.reportservice.report.finances.sum.service;

import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.report.dto.ReportDto;
import com.finovara.contracts.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ReportSummaryService {

    private final FinanceBackendReportClient reportClient;

    @Cacheable(value = "report:sumExpense", key = "#userId + ':' + #periodType")
    public ReportDto sumExpense(Long userId, PeriodType periodType) {
        LocalDate to = LocalDate.now();
        LocalDate from = periodType.getStartDate(to);
        BigDecimal amount = reportClient.sumExpenses(userId, from, to);
        return new ReportDto(periodType, amount);
    }

    @Cacheable(value = "report:sumRevenue", key = "#userId + ':' + #periodType")
    public ReportDto sumRevenue(Long userId, PeriodType periodType) {
        LocalDate to = LocalDate.now();
        LocalDate from = periodType.getStartDate(to);
        BigDecimal amount = reportClient.sumRevenues(userId, from, to);
        return new ReportDto(periodType, amount);
    }
}