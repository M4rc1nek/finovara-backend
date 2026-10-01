package com.finovara.activitylogservice.internal.security.mainaccount.digest.dto;

import java.util.List;

public record LoginDigestSummaryDto(
        long successfulLogins,
        long failedLogins,
        List<String> ipAddresses,
        List<String> locations,
        List<String> browsers
) {
}
