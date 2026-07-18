package com.finovara.financeservice.sharedaccount.limit.mapper;

import com.finovara.financeservice.limit.model.LimitStatus;
import com.finovara.financeservice.sharedaccount.limit.dto.SharedLimitStatsDto;
import com.finovara.financeservice.sharedaccount.limit.model.SharedLimit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class SharedLimitMapper {
    public SharedLimitStatsDto mapLimitStatsToDto(SharedLimit limit, BigDecimal spent, BigDecimal remaining, BigDecimal percentage, LimitStatus status, LocalDate date) {
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
