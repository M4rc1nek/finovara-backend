package com.finovara.financeservice.settings.finances.expense.smartscan.dto;

public record SmartScanDto(
        Boolean smartScanEnabled,
        String authorizationCode
) {
}
