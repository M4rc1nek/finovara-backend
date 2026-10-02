package com.finovara.reportservice.report.finances.smartreport.service.handler;

import com.finovara.contracts.util.calculate.CalculatePercentage;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.report.finances.smartreport.model.SmartReportType;
import com.finovara.reportservice.report.finances.smartreport.service.SmartReportHandler;
import com.finovara.reportservice.report.finances.smartreport.service.loader.SmartReportTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SavingsRateHandler implements SmartReportHandler {

    private final FinanceBackendReportClient reportClient;
    private final SmartReportTemplateService templateService;

    @Override
    public SmartReportType getType() {
        return SmartReportType.SAVINGS_RATE;
    }

    @Override
    public String generate(Long userId) {
        BigDecimal revenues = Optional.ofNullable(reportClient.sumAllRevenues(userId))
                .orElse(BigDecimal.ZERO);
        BigDecimal expenses = Optional.ofNullable(reportClient.sumAllExpenses(userId))
                .orElse(BigDecimal.ZERO);

        BigDecimal savings = revenues.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : CalculatePercentage.calculatePercentage(
                revenues.subtract(expenses), revenues);

        String template = templateService.getRandomResponse(SmartReportType.SAVINGS_RATE);
        return template.replace("{amount}", savings.toString());
    }
}