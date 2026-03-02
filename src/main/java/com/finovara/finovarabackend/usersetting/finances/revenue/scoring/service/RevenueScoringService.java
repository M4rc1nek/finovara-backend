package com.finovara.finovarabackend.usersetting.finances.revenue.scoring.service;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.revenue.model.RevenueSettings;
import com.finovara.finovarabackend.usersetting.finances.revenue.scoring.dto.RevenueScoringDto;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenueScoringService {

    private final UserManagerService userManagerService;
    private final RevenueRepository revenueRepository;
    private final ExpenseRepository expenseRepository;

    private static final BigDecimal POINTS_TO_ADD = BigDecimal.valueOf(1);
    private static final BigDecimal POINTS_TO_REMOVE = BigDecimal.valueOf(1);

    @Transactional
    public void saveScoringIncome(String email, RevenueScoringDto dto) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        RevenueSettings settings = user.getRevenueSettings();
        settings.setScoringEnable(dto.scoringEnable());
    }

    @Transactional
    public void recalculateScore(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        RevenueSettings settings = user.getRevenueSettings();

        if (!settings.isScoringEnable()) {
            settings.setRevenuePoints(BigDecimal.ZERO);
            return;
        }

        List<Revenue> revenues = revenueRepository.findAllByUserAssignedId(user.getId());
        List<Expense> expenses = expenseRepository.findAllByUserAssignedId(user.getId());

        BigDecimal totalPoints = BigDecimal.ZERO;

        BigDecimal averageRevenue = calculateAverageAmount(revenues.stream().map(Revenue::getAmount).toList());
        BigDecimal averageExpense = calculateAverageAmount(expenses.stream().map(Expense::getAmount).toList());

        for (Revenue revenue : revenues) {
            BigDecimal revenueAmount = revenue.getAmount();

            if (revenueAmount.compareTo(averageRevenue) > 0) totalPoints = totalPoints.add(POINTS_TO_ADD);
            if (revenueAmount.compareTo(averageExpense) > 0) totalPoints = totalPoints.add(POINTS_TO_ADD);
            if (revenueAmount.compareTo(averageExpense) < 0) totalPoints = totalPoints.subtract(POINTS_TO_REMOVE);
        }

        settings.setRevenuePoints(totalPoints.max(BigDecimal.ZERO));
    }

    public RevenueScoringDto getScoringIncome(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        RevenueSettings settings = user.getRevenueSettings();
        return new RevenueScoringDto(settings.isScoringEnable(), settings.getRevenuePoints());
    }

    private BigDecimal calculateAverageAmount(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }
}