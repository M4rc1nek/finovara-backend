package com.finovara.notificationservice.notification.dto.limit;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.finovara.notificationservice.notification.dto.NotificationResponse;
import com.finovara.contracts.model.NotificationType;
import com.finovara.contracts.model.PeriodType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonTypeName("LIMIT_EXCEEDED")
public record LimitExceededDto(
        NotificationType type,
        LocalDateTime createdAt,
        PeriodType period,
        Long limitId,
        BigDecimal threshold
) implements NotificationResponse {

    @Override
    public String deduplicationKey() {
        return "%s:%d:%s:%s".formatted(type, limitId, period, threshold);
    }

}
