package com.finovara.finovarabackend.notification.service.piggybank;

import com.finovara.finovarabackend.notification.dto.NotificationResponse;
import com.finovara.finovarabackend.notification.dto.piggybank.PiggyBankWarningDto;
import com.finovara.finovarabackend.notification.model.NotificationType;
import com.finovara.finovarabackend.notification.source.NotificationCreator;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.util.piggybank.PiggyBankCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PiggyBankReachedService implements NotificationCreator {
    private static final BigDecimal REACHED_THRESHOLD = BigDecimal.valueOf(100);

    private final PiggyBankRepository piggyBankRepository;

    @Override
    public List<NotificationResponse> getNotifications(Long userId) {
        List<NotificationResponse> result = new ArrayList<>();
        List<PiggyBank> piggyBanks = piggyBankRepository.findAllByUserAssignedId(userId);
        for (PiggyBank piggyBank : piggyBanks) {
            Double progress = PiggyBankCalculator.calculateProgress(piggyBank);
            BigDecimal percentage = BigDecimal.valueOf(progress).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
            if (percentage.compareTo(REACHED_THRESHOLD) >= 0) {
                result.add(new PiggyBankWarningDto(
                        NotificationType.PIGGY_BANK_GOAL_REACHED,
                        LocalDate.now(),
                        piggyBank.getGoalType(),
                        piggyBank.getName(),
                        piggyBank.getId(),
                        REACHED_THRESHOLD
                ));
            }
        }
        return result;

    }
}
