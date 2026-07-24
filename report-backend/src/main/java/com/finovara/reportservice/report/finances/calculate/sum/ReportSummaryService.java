package com.finovara.reportservice.report.finances.calculate.sum;

import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.util.dto.ReportDto;
import com.finovara.contracts.model.PeriodType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ReportSummaryService {

    private final FinanceBackendReportClient reportClient;
    private final Clock clock;

    @Cacheable(value = "report:sumExpense", key = "#userId + ':' + #periodType")
    public ReportDto sumExpense(Long userId, PeriodType periodType) {
        LocalDate to = LocalDate.now(clock);
        LocalDate from = periodType.getStartDate(to);
        BigDecimal amount = reportClient.sumExpenses(userId, from, to);
        return new ReportDto(periodType, amount);
    }

    @Cacheable(value = "report:sumRevenue", key = "#userId + ':' + #periodType")
    public ReportDto sumRevenue(Long userId, PeriodType periodType) {
        LocalDate to = LocalDate.now(clock);
        LocalDate from = periodType.getStartDate(to);
        BigDecimal amount = reportClient.sumRevenues(userId, from, to);
        return new ReportDto(periodType, amount);
    }

    @Cacheable(value = "report:sumAllExpense", key = "#userId")
    public BigDecimal sumAllExpenses(Long userId) {
        return reportClient.sumAllExpenses(userId);
    }

    @Cacheable(value = "report:sumAllRevenue", key = "#userId")
    public BigDecimal sumAllRevenues(Long userId) {
        return reportClient.sumAllRevenues(userId);
    }
}