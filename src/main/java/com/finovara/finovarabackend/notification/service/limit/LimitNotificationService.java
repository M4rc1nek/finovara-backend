package com.finovara.finovarabackend.notification.service.limit;

import com.finovara.finovarabackend.limit.dto.LimitStatsDto;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.limit.repository.LimitRepository;
import com.finovara.finovarabackend.limit.service.LimitCalculateService;
import com.finovara.finovarabackend.notification.dto.NotificationDto;
import com.finovara.finovarabackend.notification.model.NotificationType;
import com.finovara.finovarabackend.notification.source.NotificationCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LimitNotificationService implements NotificationCreator {
    private static final BigDecimal WARNING_THRESHOLD = BigDecimal.valueOf(75);

    private final LimitCalculateService limitCalculateService;
    private final LimitRepository limitRepository;

    @Override
    public List<NotificationDto> getNotifications(Long userId) {
        List<NotificationDto> result = new ArrayList<>();
        List<Limit> limits = limitRepository.findAllByUserAssignedId(userId);
        for (Limit limit : limits) {
            LimitStatsDto stats = limitCalculateService.calculateLimitStats(userId, limit.getId(), LocalDate.now());
            if (stats.percentage().compareTo(WARNING_THRESHOLD) >= 0) {
                result.add(new NotificationDto(
                        NotificationType.LIMIT_EXCEEDED_WARNING,
                        stats.date(),
                        stats.percentage(),
                        stats.periodType()
                ));
            }
        }
        return result;

    }
}
