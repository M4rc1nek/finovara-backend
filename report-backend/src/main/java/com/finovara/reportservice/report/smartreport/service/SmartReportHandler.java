package com.finovara.reportservice.report.smartreport.service;

import com.finovara.reportservice.report.smartreport.model.SmartReportType;

public interface SmartReportHandler {
    SmartReportType getType();
    String generate(Long userId);
}
