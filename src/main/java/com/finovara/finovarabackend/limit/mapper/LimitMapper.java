package com.finovara.finovarabackend.limit.mapper;

import com.finovara.finovarabackend.limit.dto.LimitStatsDto;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.limit.model.LimitStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class LimitMapper {
    public LimitStatsDto mapLimitStatsToDto(Limit limit, BigDecimal spent, BigDecimal remaining, BigDecimal percentage, LimitStatus status, LocalDate date) {
        return new LimitStatsDto(
                limit.getId(),
                limit.getLimitType(),
                limit.getAmount(),
                spent,
                remaining,
                percentage,
                status,
                date

        );
    }
}

