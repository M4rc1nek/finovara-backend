package com.finovara.corebackend.notification.service.limit;

import com.finovara.corebackend.limit.dto.LimitStatsDto;
import com.finovara.corebackend.limit.model.Limit;
import com.finovara.corebackend.limit.repository.LimitRepository;
import com.finovara.corebackend.limit.service.LimitCalculateService;
import com.finovara.corebackend.notification.dto.NotificationResponse;
import com.finovara.corebackend.notification.dto.limit.LimitExceededDto;
import com.finovara.corebackend.notification.model.NotificationType;
import com.finovara.corebackend.notification.service.core.ThresholdReachedService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LimitExceededService extends ThresholdReachedService<Limit, LimitStatsDto> {

    private final LimitRepository limitRepository;
    private final LimitCalculateService limitCalculateService;

    @Override
    protected List<Limit> fetchEntities(Long userId) {
        return limitRepository.findAllByUserAssignedId(userId);
    }

    @Override
    protected LimitStatsDto calculate(Limit entity, Long userId) {
        return limitCalculateService.calculateLimitStats(entity, userId, LocalDate.now());
    }

    @Override
    protected BigDecimal getPercentage(LimitStatsDto context) {
        return context.percentage();
    }

    @Override
    protected NotificationResponse buildNotification(Limit entity, LimitStatsDto context, Long userId) {
        return new LimitExceededDto(
                NotificationType.LIMIT_EXCEEDED,
                LocalDateTime.now(),
                context.periodType(),
                context.limitId(),
                context.percentage()
        );
    }
}