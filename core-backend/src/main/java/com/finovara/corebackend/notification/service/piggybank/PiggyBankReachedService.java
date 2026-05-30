package com.finovara.corebackend.notification.service.piggybank;

import com.finovara.corebackend.notification.dto.NotificationResponse;
import com.finovara.corebackend.notification.dto.piggybank.PiggyBankReachedDto;
import com.finovara.corebackend.notification.model.NotificationType;
import com.finovara.corebackend.notification.service.core.ThresholdReachedService;
import com.finovara.corebackend.piggybank.model.PiggyBank;
import com.finovara.corebackend.piggybank.repository.PiggyBankRepository;
import com.finovara.corebackend.util.piggybank.PiggyBankCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PiggyBankReachedService extends ThresholdReachedService<PiggyBank, BigDecimal> {

    private final PiggyBankRepository piggyBankRepository;

    @Override
    protected List<PiggyBank> fetchEntities(Long userId) {
        return piggyBankRepository.findAllByUserAssignedId(userId);
    }

    @Override
    protected BigDecimal calculate(PiggyBank entity, Long userId) {
        Double progress = PiggyBankCalculator.calculateProgress(entity);

        return BigDecimal.valueOf(progress)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    protected BigDecimal getPercentage(BigDecimal context) {
        return context;
    }

    @Override
    protected NotificationResponse buildNotification(PiggyBank entity, BigDecimal percentage, Long userId) {
        return new PiggyBankReachedDto(
                NotificationType.PIGGY_BANK_GOAL_REACHED,
                LocalDateTime.now(),
                entity.getGoalType(),
                entity.getName(),
                entity.getId(),
                percentage
        );
    }
}