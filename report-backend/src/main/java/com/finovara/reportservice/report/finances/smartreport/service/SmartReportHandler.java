package com.finovara.reportservice.report.finances.smartreport.service;

import com.finovara.reportservice.report.finances.smartreport.model.SmartReportType;

public interface SmartReportHandler {
    SmartReportType getType();
    String generate(Long userId);
}
