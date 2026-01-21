package com.finovara.finovarabackend.limit.mapper;

import com.finovara.finovarabackend.limit.dto.LimitStatsDTO;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.limit.model.LimitStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class LimitMapper {
    public LimitStatsDTO mapLimitStatsToDTO(Limit limit, BigDecimal spent, BigDecimal remaining, BigDecimal percentage, LimitStatus status, LocalDate date) {
        return new LimitStatsDTO(
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

