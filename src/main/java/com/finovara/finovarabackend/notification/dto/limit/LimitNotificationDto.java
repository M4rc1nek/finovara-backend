package com.finovara.finovarabackend.notification.dto.limit;

import com.finovara.finovarabackend.notification.dto.NotificationDto;
import com.finovara.finovarabackend.notification.model.NotificationType;
import com.finovara.finovarabackend.util.model.PeriodType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LimitNotificationDto (
        NotificationType type,
        LocalDate createdAt,
        BigDecimal limitPercentage,
        PeriodType limitPeriodType
) implements NotificationDto {}
