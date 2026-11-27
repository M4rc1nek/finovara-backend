package com.finovara.reportservice.savingsplan.service;

import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.savingsplan.dto.request.SavingsPlanRequest;
import com.finovara.reportservice.savingsplan.dto.response.SavingsPlanReport;
import com.finovara.reportservice.savingsplan.service.calculation.SavingsPlanCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavingsPlanService {

    private final FinanceBackendReportClient financeBackendReportClient;
    private final SavingsPlanCalculator savingsPlanCalculator;

    public SavingsPlanReport createReport(Long userId, SavingsPlanRequest request) {
        log.info("Starting creation of savings plan report for userId={}", userId);
        SavingsPlanReport report = savingsPlanCalculator.calculate(request, financeBackendReportClient.getSavingsPlanMonthlyData(userId), LocalDate.now());
        log.info("Created savings plan report for userId={}, result={}, dataQuality={}", userId, report.result(), report.dataQuality());
        return report;
    }
}
