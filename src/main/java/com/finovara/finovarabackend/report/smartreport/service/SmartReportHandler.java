package com.finovara.finovarabackend.report.smartreport.service;

import com.finovara.finovarabackend.report.smartreport.model.SmartReportType;

public interface SmartReportHandler {
    SmartReportType getType();
    String generate(Long userId);
}
