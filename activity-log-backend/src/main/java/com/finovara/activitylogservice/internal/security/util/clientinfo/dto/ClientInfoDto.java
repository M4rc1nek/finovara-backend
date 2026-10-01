package com.finovara.activitylogservice.internal.security.util.clientinfo;

import com.finovara.contracts.report.dto.ShareStatDto;

import java.util.List;

public record ClientInfoDto(
        List<ShareStatDto> locationShares,
        List<ShareStatDto> browserShares
) {
}
