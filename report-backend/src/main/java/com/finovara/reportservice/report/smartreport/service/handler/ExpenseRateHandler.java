package com.finovara.reportservice.report.smartreport.service.handler;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.calculate.CalculatePercentage;
import com.finovara.reportservice.report.smartreport.model.SmartReportType;
import com.finovara.reportservice.report.smartreport.service.SmartReportHandler;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ExpenseRateHandler implements SmartReportHandler {

    @Override
    public SmartReportType getType() {
        return SmartReportType.EXPENSE_RATE;
    }

    @Override
    public PeriodType getPeriodType() {
        return PeriodType.MONTHLY;
    }

    @Override
    public BigDecimal calculate(FinancialReportDto report) {
        BigDecimal revenues = ReportValues.revenues(report);
        if (revenues.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return CalculatePercentage.calculatePercentage(ReportValues.expenses(report), revenues);
    }
}