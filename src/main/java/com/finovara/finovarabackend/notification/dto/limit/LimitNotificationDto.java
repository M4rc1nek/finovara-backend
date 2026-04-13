package com.finovara.finovarabackend.notification.dto.limit;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.finovara.finovarabackend.notification.dto.NotificationResponse;
import com.finovara.finovarabackend.notification.model.NotificationType;
import com.finovara.finovarabackend.util.model.PeriodType;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonTypeName("LIMIT_EXCEEDED_WARNING")
public record LimitNotificationDto(
        NotificationType type,
        LocalDate createdAt,
        BigDecimal limitPercentage,
        PeriodType period,
        Long limitId,
        BigDecimal threshold
) implements NotificationResponse {

    @Override
    public String deduplicationKey() {
        return "%s:%d:%s:%s".formatted(type, limitId, period, threshold);
    }
}
