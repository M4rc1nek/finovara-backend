package com.finovara.reportservice.sharedaccount.report.finances.response.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.reportservice.feignclient.FinanceBackendSharedReportClient;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SharedFinancialReportService {

    private final FinanceBackendSharedReportClient financeBackendSharedReportClient;

    @Cacheable(value = "financial-report:shared", key = "#ownerId + ':' + #memberId + ':' + #periodType")
    public FinancialReportDto buildReport(Long ownerId, Long memberId, PeriodType periodType) {
        if (periodType == null) {
            throw new InvalidInputException("Unsupported report period type.");
        }
        return financeBackendSharedReportClient.getFinancialReport(ownerId, memberId, periodType);
    }
}
