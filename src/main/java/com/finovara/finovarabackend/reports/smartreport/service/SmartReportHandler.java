package com.finovara.finovarabackend.reports.smartreport.service;

import com.finovara.finovarabackend.reports.smartreport.model.SmartReportType;

public interface SmartReportHandler {
    SmartReportType getType();
    String generate(Long userId);
}
