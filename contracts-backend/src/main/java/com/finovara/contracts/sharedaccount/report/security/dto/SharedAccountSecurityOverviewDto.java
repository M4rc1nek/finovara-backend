package com.finovara.contracts.sharedaccount.report.security.dto;

import java.util.List;

public record SharedAccountSecurityOverviewDto(
        List<SharedAccountSecurityReportDto> members
) {
}