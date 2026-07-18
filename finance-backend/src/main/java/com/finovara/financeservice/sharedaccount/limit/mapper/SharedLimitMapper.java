package com.finovara.financeservice.sharedaccount.limit.mapper;

import com.finovara.financeservice.sharedaccount.limit.dto.SharedLimitStatsDto;
import com.finovara.financeservice.sharedaccount.limit.model.Limit;
import com.finovara.financeservice.sharedaccount.limit.model.LimitStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class LimitMapper {
    public SharedLimitStatsDto mapLimitStatsToDto(Limit limit, BigDecimal spent, BigDecimal remaining, BigDecimal percentage, LimitStatus status, LocalDate date) {
        return new SharedLimitStatsDto(
                limit.getId(),
                limit.getPeriodType(),
                limit.getCategory(),
                limit.getAmount(),
                spent,
                remaining,
                percentage,
                status,
                date

        );
    }
}
