package com.finovara.finovarabackend.limit.dto;

import com.finovara.finovarabackend.limit.model.LimitStatus;
import com.finovara.finovarabackend.limit.model.LimitType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record LimitDTO(
        Long userId,
        Long id,

        LimitType limitType,
        LimitStatus limitStatus,
        @DecimalMin(value = "1.0", message = "Amount must be at least 1")
        @DecimalMax(value = "1000000.00", message = "Amount must not exceed 1 000 000")
        @Digits(integer = 7, fraction = 2) // 7 liczb przed przecinkiem, 2 po przecinku
        BigDecimal amount,

        Boolean isActive
) {
}
