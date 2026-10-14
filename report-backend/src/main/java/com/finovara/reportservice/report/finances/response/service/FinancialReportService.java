package com.finovara.reportservice.report.finances.response.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FinancialReportService {

    private final FinanceBackendReportClient financeBackendReportClient;

    @Cacheable(value = "financial-report", key = "#userId + ':' + #periodType")
    public FinancialReportDto buildReport(Long userId, PeriodType periodType) {
        if (periodType == null) {
            throw new InvalidInputException("Unsupported report period type.");
        }
        return financeBackendReportClient.getFinancialReport(userId, periodType);
    }
}