package com.finovara.notificationservice.notification.dto.recurring;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.finovara.notificationservice.notification.dto.NotificationResponse;
import com.finovara.contracts.model.NotificationType;
import com.finovara.contracts.model.RecurringType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonTypeName("RECURRING_EXECUTION_SKIPPED")
public record RecurringExecutionSkippedDto(
        NotificationType type,
        LocalDateTime createdAt,
        RecurringType recurringType,
        Long recurringSettingsId,
        BigDecimal amount,
        LocalDate lastScheduledDate,
        int skippedCount
) implements NotificationResponse {

    @Override
    public String deduplicationKey() {
        return "%s:%d:%d:%s".formatted(type, recurringSettingsId, skippedCount, lastScheduledDate);
    }
}