package com.finovara.finovarabackend.notification.service.limit;

import com.finovara.finovarabackend.limit.dto.LimitStatsDto;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.limit.repository.LimitRepository;
import com.finovara.finovarabackend.limit.service.LimitCalculateService;
import com.finovara.finovarabackend.notification.dto.NotificationResponse;
import com.finovara.finovarabackend.notification.dto.limit.LimitWarningDto;
import com.finovara.finovarabackend.notification.model.NotificationType;
import com.finovara.finovarabackend.notification.service.core.AbstractWarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LimitWarningService extends AbstractWarningService<Limit, LimitStatsDto> {

    private final LimitRepository limitRepository;
    private final LimitCalculateService limitCalculateService;

    @Override
    protected List<Limit> fetchEntities(Long userId) {
        return limitRepository.findAllByUserAssignedId(userId);
    }

    @Override
    protected LimitStatsDto calculate(Limit limit, Long userId) {
        return limitCalculateService.calculateLimitStats(
                userId,
                limit.getId(),
                LocalDate.now()
        );
    }

    @Override
    protected BigDecimal getPercentage(LimitStatsDto stats) {
        return stats.percentage();
    }

    @Override
    protected NotificationResponse buildNotification(Limit limit, LimitStatsDto stats, Long userId) {
        return new LimitWarningDto(
                NotificationType.LIMIT_EXCEEDED_WARNING,
                stats.createdAt(),
                stats.percentage(),
                stats.periodType(),
                stats.limitId(),
                WARNING_THRESHOLD
        );
    }
}