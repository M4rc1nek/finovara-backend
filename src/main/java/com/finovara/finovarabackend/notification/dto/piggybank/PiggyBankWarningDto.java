package com.finovara.finovarabackend.notification.dto.piggybank;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.finovara.finovarabackend.notification.dto.NotificationResponse;
import com.finovara.finovarabackend.notification.model.NotificationType;
import com.finovara.finovarabackend.piggybank.model.PiggyBankGoalType;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonTypeName("PIGGY_BANK_GOAL_APPROACHING")
public record PiggyBankWarningDto(
        NotificationType type,
        LocalDate createdAt,
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
