package com.finovara.notificationservice.notification.dto.piggybank;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.finovara.notificationservice.notification.dto.NotificationResponse;
import com.finovara.contracts.model.NotificationType;
import com.finovara.contracts.model.transaction.PiggyBankGoalType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonTypeName("PIGGY_BANK_GOAL_REACHED")
public record PiggyBankReachedDto(
        NotificationType type,
        LocalDateTime createdAt,
        PiggyBankGoalType goalType,
        String piggyBankName,
        Long piggyBankId,
        BigDecimal threshold
) implements NotificationResponse {

    @Override
    public String deduplicationKey() {
        return "%s:%d:%s:%s".formatted(type, piggyBankId, goalType, threshold);

    }
}
