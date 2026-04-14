package com.finovara.finovarabackend.notification.service.piggybank;

import com.finovara.finovarabackend.notification.dto.NotificationResponse;
import com.finovara.finovarabackend.notification.dto.piggybank.PiggyBankWarningDto;
import com.finovara.finovarabackend.notification.model.NotificationType;
import com.finovara.finovarabackend.notification.service.AbstractWarningService;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.util.piggybank.PiggyBankCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PiggyBankWarningService extends AbstractWarningService<PiggyBank, BigDecimal> {

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
                LocalDate.now(),
                piggyBank.getGoalType(),
                piggyBank.getName(),
                piggyBank.getId(),
                WARNING_THRESHOLD
        );
    }
}