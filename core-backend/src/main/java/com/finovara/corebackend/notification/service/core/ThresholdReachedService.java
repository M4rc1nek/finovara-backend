package com.finovara.corebackend.notification.service.core;

import com.finovara.corebackend.notification.dto.NotificationResponse;
import com.finovara.corebackend.notification.source.NotificationCreator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public abstract class ThresholdReachedService<E, C> implements NotificationCreator {
    protected static final BigDecimal REACHED_THRESHOLD = BigDecimal.valueOf(100);

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

            if (percentage.compareTo(REACHED_THRESHOLD) >= 0) {
                result.add(buildNotification(entity, context, userId));
            }
        }

        return result;
    }
}