package com.finovara.finovarabackend.usersetting.finances.revenue.scoring.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record RevenueScoringDto(
        Boolean scoringEnable,
      @DecimalMin("0") @DecimalMax("10") BigDecimal revenuePoints
) {
}
