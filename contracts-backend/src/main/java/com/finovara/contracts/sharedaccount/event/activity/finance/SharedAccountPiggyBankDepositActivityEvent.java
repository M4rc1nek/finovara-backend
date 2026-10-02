package com.finovara.contracts.finance.event.sharedaccount.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SharedAccountPiggyBankDepositActivityEvent(
        Long ownerId,
        Long memberId,
        Long userId,
        Long piggyBankId,
        BigDecimal amount,
        LocalDateTime createdAt) {}