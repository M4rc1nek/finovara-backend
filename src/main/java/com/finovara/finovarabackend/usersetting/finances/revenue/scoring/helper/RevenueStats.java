package com.finovara.finovarabackend.usersetting.finances.revenue.scoring.helper;

import java.math.BigDecimal;

public record RevenueStats(
        BigDecimal averageExpense,
        BigDecimal averageRevenue,
        BigDecimal revenueAmount
){}