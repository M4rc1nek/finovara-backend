package com.finovara.corebackend.report.smartreport.service;

import com.finovara.corebackend.report.smartreport.model.SmartReportType;

public interface SmartReportHandler {
    SmartReportType getType();
    String generate(Long userId);
}
