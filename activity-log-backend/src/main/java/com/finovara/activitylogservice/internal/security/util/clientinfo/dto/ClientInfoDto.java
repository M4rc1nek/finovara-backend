package com.finovara.activitylogservice.internal.security.util.clientinfo.dto;

import com.finovara.contracts.report.dto.security.ShareStatDto;

import java.util.List;

public record ClientInfoDto(
        List<ShareStatDto> locationShares,
        List<ShareStatDto> browserShares
) {
}
