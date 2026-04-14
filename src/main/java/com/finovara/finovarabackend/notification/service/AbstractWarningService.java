package com.finovara.finovarabackend.notification.service;

import com.finovara.finovarabackend.notification.dto.NotificationResponse;
import com.finovara.finovarabackend.notification.source.NotificationCreator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractWarningService<E, C> implements NotificationCreator {

    protected static final BigDecimal WARNING_THRESHOLD = BigDecimal.valueOf(75);
    protected static final BigDecimal BLOCK_THRESHOLD = BigDecimal.valueOf(100);

    protected abstract List<E> fetchEntities(Long userId);

    protected abstract C calculate(E entity, Long userId);

    protected abstract BigDecimal getPercentage(C context);

    protected abstract NotificationResponse buildNotification(E entity, C context, Long userId);

    @Override
    public List<NotificationResponse> getNotifications(Long userId) {
        List<NotificationResponse> result = new ArrayList<>();

        for (E entity : fetchEntities(userId)) {
            C context = calculate(entity, userId);
            BigDecimal percentage = getPercentage(context);

            boolean isWarning = percentage.compareTo(WARNING_THRESHOLD) >= 0;
            boolean isBelowLimit = percentage.compareTo(BLOCK_THRESHOLD) < 0;

            if (isWarning && isBelowLimit) {
                result.add(buildNotification(entity, context, userId));
            }
        }

        return result;
    }
}