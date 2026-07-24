package com.finovara.financeservice.sharedaccount.revenue.dto;

public record SharedRevenueResponse(
        Long revenueId,
        Long userId,
        String username
) {
}
