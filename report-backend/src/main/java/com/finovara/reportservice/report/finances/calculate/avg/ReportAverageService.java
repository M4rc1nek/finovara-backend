package com.finovara.reportservice.report.finances.average.service;

import com.finovara.contracts.model.PeriodType;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.report.dto.ReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ReportAverageService {

    private final FinanceBackendReportClient reportClient;

    @Cacheable(value = "report:avgExpense", key = "#userId + ':' + #periodType")
    public ReportDto calculateAverageExpense(Long userId, PeriodType periodType) {
        LocalDate to = LocalDate.now();
        LocalDate from = periodType.getStartDate(to);
        BigDecimal amount = reportClient.avgExpenses(userId, from, to);
        return new ReportDto(periodType, amount);
    }

    @Cacheable(value = "report:avgRevenue", key = "#userId + ':' + #periodType")
    public ReportDto calculateAverageRevenue(Long userId, PeriodType periodType) {
        LocalDate to = LocalDate.now();
        LocalDate from = periodType.getStartDate(to);
        BigDecimal amount = reportClient.avgRevenues(userId, from, to);
        return new ReportDto(periodType, amount);
    }
}