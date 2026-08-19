package com.finovara.financeservice.internal.digest.report.email.service;

import com.finovara.contracts.notification.email.digest.report.PiggyBankSummaryDto;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import com.finovara.financeservice.util.piggybank.PiggyBankCheckGoalCompletion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PiggyBankDigestService {

    private final PiggyBankRepository piggyBankRepository;

    public PiggyBankSummaryDto calculateSummary(Long userId, LocalDate from, LocalDate to) {
        List<PiggyBank> piggyBanks = piggyBankRepository.findAllByUserId(userId);

        return new PiggyBankSummaryDto(
                quantityOfPiggyBanks(userId, from, to),
                calculateTotalDepositedMoney(piggyBanks),
                calculateOverallProgress(piggyBanks),
                calculateRemainingAmount(piggyBanks),
                allGoalsCompleted(piggyBanks)
        );
    }

    private long quantityOfPiggyBanks(Long userId, LocalDate from, LocalDate to) {
        return piggyBankRepository.countPiggyBanksByUserIdAndCreatedAtBetween(userId, from, to);
    }

    private BigDecimal calculateTotalDepositedMoney(List<PiggyBank> piggyBanks) {
        return piggyBanks.stream()
                .map(PiggyBank::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateOverallProgress(List<PiggyBank> piggyBanks) {
        BigDecimal totalGoalAmount = piggyBanks.stream()
                .map(PiggyBank::getGoalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalGoalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalSaved = calculateTotalDepositedMoney(piggyBanks);

        return totalSaved
                .divide(totalGoalAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRemainingAmount(List<PiggyBank> piggyBanks) {
        return piggyBanks.stream()
                .filter(piggyBank -> piggyBank.getGoalAmount() != null)
                .map(piggyBank -> piggyBank.getGoalAmount().subtract(piggyBank.getAmount()).max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean allGoalsCompleted(List<PiggyBank> piggyBanks) {
        List<PiggyBank> piggyBanksWithGoal = piggyBanks.stream()
                .filter(piggyBank -> piggyBank.getGoalAmount() != null)
                .toList();

        if (piggyBanksWithGoal.isEmpty()) {
            return false;
        }

        return piggyBanksWithGoal.stream().allMatch(PiggyBankCheckGoalCompletion::isGoalCompleted);
    }
}