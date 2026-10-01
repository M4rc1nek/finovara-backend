package com.finovara.activitylogservice.internal.security.mainaccount.report;

import com.finovara.activitylogservice.internal.security.mainaccount.report.dto.ReportAccountChangeDto;
import com.finovara.activitylogservice.internal.security.mainaccount.report.dto.ReportLoginDto;
import com.finovara.activitylogservice.internal.security.mainaccount.report.mapper.SecurityReportMapper;
import com.finovara.activitylogservice.internal.security.mainaccount.report.service.SecurityReportAccountChangeService;
import com.finovara.activitylogservice.internal.security.mainaccount.report.service.SecurityReportLoginService;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.report.dto.security.SecurityReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InternalSecurityReportService {
    private final SecurityReportAccountChangeService securityReportAccountChangeService;
    private final SecurityReportLoginService securityReportLoginService;
    private final SecurityReportMapper securityReportMapper;

    public SecurityReportDto getSummaryForUser(Long userId, PeriodType periodType) {
        ReportAccountChangeDto reportAccountChangeDto = securityReportAccountChangeService.getAccountChangesSummary(userId, periodType);
        ReportLoginDto reportLoginDto = securityReportLoginService.getLoginSummary(userId, periodType);
        return securityReportMapper.toDto(userId, reportLoginDto, reportAccountChangeDto);
    }
}
