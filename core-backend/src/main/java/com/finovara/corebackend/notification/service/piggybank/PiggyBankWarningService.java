package com.finovara.corebackend.notification.service.piggybank;

import com.finovara.corebackend.notification.dto.NotificationResponse;
import com.finovara.corebackend.notification.dto.piggybank.PiggyBankWarningDto;
import com.finovara.corebackend.notification.model.NotificationType;
import com.finovara.corebackend.notification.service.core.ThresholdWarningService;
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
public class PiggyBankWarningService extends ThresholdWarningService<PiggyBank, BigDecimal> {

    private final PiggyBankRepository piggyBankRepository;

    @Override
    protected List<PiggyBank> fetchEntities(Long userId) {
        return piggyBankRepository.findAllByUserAssignedId(userId);
    }

    @Override
    protected BigDecimal calculate(PiggyBank piggyBank, Long userId) {
        Double progress = PiggyBankCalculator.calculateProgress(piggyBank);

        if (progress == null) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(progress)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    protected BigDecimal getPercentage(BigDecimal percentage) {
        return percentage;
    }

    @Override
    protected NotificationResponse buildNotification(PiggyBank piggyBank, BigDecimal percentage, Long userId) {
        return new PiggyBankWarningDto(
                NotificationType.PIGGY_BANK_GOAL_APPROACHING,
                LocalDateTime.now(),
                piggyBank.getGoalType(),
                piggyBank.getName(),
                piggyBank.getId(),
                WARNING_THRESHOLD
        );
    }
}