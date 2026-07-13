package com.finovara.reportservice.sharedaccount.report.finances.calculate.sum.service;

import com.finovara.contracts.model.PeriodType;
import com.finovara.reportservice.feignclient.FinanceBackendSharedReportClient;
import com.finovara.reportservice.util.dto.ReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SharedReportSummaryService {

    private final FinanceBackendSharedReportClient reportClient;
    private final Clock clock;

    @Cacheable(value = "report:sharedSumExpense", key = "#ownerId + ':' + #memberId + ':' + #periodType")
    public ReportDto sumExpense(Long ownerId, Long memberId, PeriodType periodType) {
        LocalDate to = LocalDate.now(clock);
        LocalDate from = periodType.getStartDate(to);
        BigDecimal amount = reportClient.sumExpenses(ownerId, memberId, from, to);
        return new ReportDto(periodType, amount);
    }

    @Cacheable(value = "report:sharedSumRevenue", key = "#ownerId + ':' + #memberId + ':' + #periodType")
    public ReportDto sumRevenue(Long ownerId, Long memberId, PeriodType periodType) {
        LocalDate to = LocalDate.now(clock);
        LocalDate from = periodType.getStartDate(to);
        BigDecimal amount = reportClient.sumRevenues(ownerId, memberId, from, to);
        return new ReportDto(periodType, amount);
    }

    @Cacheable(value = "report:sharedSumAllExpense", key = "#ownerId + ':' + #memberId")
    public BigDecimal sumAllExpenses(Long ownerId, Long memberId) {
        return reportClient.sumAllExpenses(ownerId, memberId);
    }

    @Cacheable(value = "report:sharedSumAllRevenue", key = "#ownerId + ':' + #memberId")
    public BigDecimal sumAllRevenues(Long ownerId, Long memberId) {
        return reportClient.sumAllRevenues(ownerId, memberId);
    }
}