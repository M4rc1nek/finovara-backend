package com.finovara.contracts.report.dto.security;

import java.util.List;

public record SharedAccountSecurityOverviewDto(
        List<SharedAccountSecurityReportDto> members
) {
}