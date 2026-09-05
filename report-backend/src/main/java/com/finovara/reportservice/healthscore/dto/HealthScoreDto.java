package com.finovara.reportservice.healthscore.dto;

import com.finovara.reportservice.healthscore.model.HealthScoreStatus;

import java.math.BigDecimal;

public record HealthScoreDto(
        BigDecimal finalScore,
        HealthScoreStatus statusFromScore
) {
}
