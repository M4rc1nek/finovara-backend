package com.finovara.reportservice.report.smartreport.service.handler;

import com.finovara.reportservice.feignclient.CoreBackendReportClient;
import com.finovara.reportservice.report.smartreport.model.SmartReportType;
import com.finovara.reportservice.report.smartreport.service.SmartReportHandler;
import com.finovara.reportservice.report.smartreport.service.loader.SmartReportTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AverageDaySpendingHandler implements SmartReportHandler {

    private final CoreBackendReportClient reportClient;
    private final SmartReportTemplateService templateService;

    @Override
    public SmartReportType getType() {
        return SmartReportType.AVERAGE_DAY_SPENDING;
    }

    @Override
    public String generate(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startMonth = today.withDayOfMonth(1);
        long days = ChronoUnit.DAYS.between(startMonth, today) + 1;

        BigDecimal sum = reportClient.sumExpenses(userId, startMonth, today);
        BigDecimal average = sum.divide(BigDecimal.valueOf(days), RoundingMode.HALF_UP);

        String template = templateService.getRandomResponse(SmartReportType.AVERAGE_DAY_SPENDING);
        return template.replace("{amount}", average.toString());
    }
}