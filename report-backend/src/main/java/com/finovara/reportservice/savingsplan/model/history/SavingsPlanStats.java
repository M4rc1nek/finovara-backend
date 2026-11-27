package com.finovara.reportservice.savingsplan.model.history;

import com.finovara.reportservice.savingsplan.dto.response.model.DataQuality;
import java.math.BigDecimal;

public record SavingsPlanStats(
        BigDecimal monthlyRevenue,
        BigDecimal monthlyExpenses,
        BigDecimal freeMoneyPerMonth,
        DataQuality historyQuality
) {
}
