package com.finovara.finovarabackend.notification.service.limit;

import com.finovara.finovarabackend.limit.dto.LimitStatsDto;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.limit.repository.LimitRepository;
import com.finovara.finovarabackend.limit.service.LimitCalculateService;
import com.finovara.finovarabackend.notification.dto.NotificationResponse;
import com.finovara.finovarabackend.notification.dto.limit.LimitExceededDto;
import com.finovara.finovarabackend.notification.model.NotificationType;
import com.finovara.finovarabackend.notification.service.core.ThresholdReachedService;
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
        return limitCalculateService.calculateLimitStats(userId, entity.getId(), LocalDate.now());
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