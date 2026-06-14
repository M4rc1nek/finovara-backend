package com.finovara.reportservice.report.smartreport.service.handler;

import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.report.smartreport.model.SmartReportType;
import com.finovara.reportservice.report.smartreport.service.SmartReportHandler;
import com.finovara.reportservice.report.smartreport.service.loader.SmartReportTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MonthSpendingHandler implements SmartReportHandler {

    private final FinanceBackendReportClient reportClient;
    private final SmartReportTemplateService templateService;

    @Override
    public SmartReportType getType() {
        return SmartReportType.MONTH_SPENDING;
    }

    @Override
    public String generate(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startMonth = today.withDayOfMonth(1);
        BigDecimal sum = reportClient.sumExpenses(userId, startMonth, today);
        String template = templateService.getRandomResponse(SmartReportType.MONTH_SPENDING);
        return template.replace("{amount}", sum.toString());
    }
}