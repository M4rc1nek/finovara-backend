package com.finovara.finovarabackend.notification.dto;

import com.finovara.finovarabackend.notification.model.NotificationType;
import com.finovara.finovarabackend.util.model.PeriodType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NotificationDto(
        NotificationType type,
        BigDecimal limitPercentage,
        PeriodType limitPeriodType,

        LocalDate date
) {
}
