package com.finovara.contracts.notification.event.recurring.transaction;

import com.finovara.contracts.model.RecurringType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record RecurringExecutionSkippedEvent(
        Long userId,
        RecurringType type,
        Long recurringSettingsId,
        BigDecimal amount,
        LocalDate lastScheduledDate,
        int skippedCount,
        LocalDateTime occurredAt
) {
}