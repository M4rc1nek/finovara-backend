package com.finovara.reportservice.report.smartreport.service.handler;

import com.finovara.contracts.percentage.CalculatePercentage;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.report.smartreport.model.SmartReportType;
import com.finovara.reportservice.report.smartreport.service.SmartReportHandler;
import com.finovara.reportservice.report.smartreport.service.loader.SmartReportTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExpenseRateHandler implements SmartReportHandler {

    private final FinanceBackendReportClient reportClient;
    private final SmartReportTemplateService templateService;

    @Override
    public SmartReportType getType() {
        return SmartReportType.EXPENSE_RATE;
    }

    @Override
    public String generate(Long userId) {
        BigDecimal expenses = Optional.ofNullable(reportClient.sumAllExpenses(userId))
                .orElse(BigDecimal.ZERO);
        BigDecimal revenues = Optional.ofNullable(reportClient.sumAllRevenues(userId))
                .orElse(BigDecimal.ZERO);

        BigDecimal rate = CalculatePercentage.calculatePercentage(expenses, revenues);
        String template = templateService.getRandomResponse(SmartReportType.EXPENSE_RATE);
        return template.replace("{amount}", rate.setScale(2, RoundingMode.HALF_UP).toString());
    }
}