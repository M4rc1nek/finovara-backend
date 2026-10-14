package com.finovara.reportservice.report.smartreport.service;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.reportservice.report.smartreport.model.SmartReportType;

import java.math.BigDecimal;

public interface SmartReportHandler {

    SmartReportType getType();

    PeriodType getPeriodType();

    BigDecimal calculate(FinancialReportDto report);
}