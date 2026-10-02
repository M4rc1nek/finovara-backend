package com.finovara.contracts.report.dto.security;

import com.finovara.contracts.mainaccount.report.security.dto.ShareStatDto;
import com.finovara.contracts.sharedaccount.SharedRole;

import java.time.LocalDateTime;
import java.util.List;

public record SharedAccountSecurityReportDto(
        Long userId,
        String userName,
        SharedRole role,
        List<ShareStatDto> locationShares,
        List<ShareStatDto> browserShares,
        int expensesCount,
        LocalDateTime lastExpenseDate,
        int revenuesCount,
        LocalDateTime lastRevenueDate,
        int piggyBankDepositsCount,
        LocalDateTime lastPiggyBankDepositDate,
        LocalDateTime lastActiveAt
) {
}