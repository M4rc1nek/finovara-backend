package com.finovara.reportservice.report.smartreport.service.handler;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.reportservice.report.smartreport.model.SmartReportType;
import com.finovara.reportservice.report.smartreport.service.SmartReportHandler;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MonthSpendingHandler implements SmartReportHandler {

    @Override
    public SmartReportType getType() {
        return SmartReportType.MONTH_SPENDING;
    }

    @Override
    public PeriodType getPeriodType() {
        return PeriodType.MONTHLY;
    }

    @Override
    public BigDecimal calculate(FinancialReportDto report) {
        return ReportValues.expenses(report);
    }
}