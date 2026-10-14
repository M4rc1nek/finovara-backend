package com.finovara.reportservice.report.smartreport.service.handler;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.reportservice.report.smartreport.model.SmartReportType;
import com.finovara.reportservice.report.smartreport.service.SmartReportHandler;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class AverageDaySpendingHandler implements SmartReportHandler {

    private static final PeriodType PERIOD = PeriodType.MONTHLY;

    @Override
    public SmartReportType getType() {
        return SmartReportType.AVERAGE_DAY_SPENDING;
    }

    @Override
    public PeriodType getPeriodType() {
        return PERIOD;
    }

    @Override
    public BigDecimal calculate(FinancialReportDto report) {
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(PERIOD.getStartDate(today), today) + 1;

        return ReportValues.expenses(report)
                .divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }
}