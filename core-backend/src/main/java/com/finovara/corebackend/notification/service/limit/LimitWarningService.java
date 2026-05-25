package com.finovara.corebackend.notification.service.limit;

import com.finovara.corebackend.limit.dto.LimitStatsDto;
import com.finovara.corebackend.limit.model.Limit;
import com.finovara.corebackend.limit.repository.LimitRepository;
import com.finovara.corebackend.limit.service.LimitCalculateService;
import com.finovara.corebackend.notification.dto.NotificationResponse;
import com.finovara.corebackend.notification.dto.limit.LimitWarningDto;
import com.finovara.corebackend.notification.model.NotificationType;
import com.finovara.corebackend.notification.service.core.ThresholdWarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LimitWarningService extends ThresholdWarningService<Limit, LimitStatsDto> {

    private final LimitRepository limitRepository;
    private final LimitCalculateService limitCalculateService;

    @Override
    protected List<Limit> fetchEntities(Long userId) {
        return limitRepository.findAllByUserAssignedId(userId);
    }

    @Override
    protected LimitStatsDto calculate(Limit limit, Long userId) {
        return limitCalculateService.calculateLimitStats(userId, limit.getId(), LocalDate.now());
    }

    @Override
    protected BigDecimal getPercentage(LimitStatsDto stats) {
        return stats.percentage();
    }

    @Override
    protected NotificationResponse buildNotification(Limit limit, LimitStatsDto stats, Long userId) {
        return new LimitWarningDto(
                NotificationType.LIMIT_EXCEEDED_WARNING,
                LocalDateTime.now(),
                stats.percentage(),
                stats.periodType(),
                stats.limitId(),
                WARNING_THRESHOLD
        );
    }
}