package com.finovara.activitylogservice.internal.security.util.clientinfo.dto;

import com.finovara.contracts.mainaccount.report.security.dto.ShareStatDto;

import java.util.List;

public record ClientInfoDto(
        List<ShareStatDto> locationShares,
        List<ShareStatDto> browserShares
) {
}
