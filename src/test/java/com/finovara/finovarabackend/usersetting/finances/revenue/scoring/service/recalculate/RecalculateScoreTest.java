package com.finovara.finovarabackend.usersetting.finances.revenue.scoring.service.recalculate;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.revenue.model.RevenueSettings;
import com.finovara.finovarabackend.usersetting.finances.revenue.scoring.service.RevenueScoringService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecalculateScoreTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private RevenueRepository revenueRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private RevenueScoringService revenueScoringService;

    private User user;
    private RevenueSettings revenueSettings;
    private final String EMAIL = "test@test.com";

    @BeforeEach
    void setup() {
        user = new User();
        revenueSettings = new RevenueSettings();
        user.setRevenueSettings(revenueSettings);

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
    }

    @Test
    void shouldSetZeroPointsWhenScoringDisabled() {
        revenueSettings.setScoringEnable(false);

        revenueScoringService.recalculateScore(EMAIL);

        assertEquals(BigDecimal.ZERO, revenueSettings.getRevenuePoints());
    }

    @Test
    void shouldSetZeroPointsWhenNoRevenuesOrExpenses() {
        revenueSettings.setScoringEnable(true);

        when(revenueRepository.findAllByUserAssignedId(user.getId())).thenReturn(List.of());
        when(expenseRepository.findAllByUserAssignedId(user.getId())).thenReturn(List.of());

        revenueScoringService.recalculateScore(EMAIL);

        assertEquals(BigDecimal.ZERO, revenueSettings.getRevenuePoints());
    }

    @Test
    void shouldAddPointsForRevenueAboveAverageRevenue() {
        revenueSettings.setScoringEnable(true);

        Revenue revenue1 = new Revenue(); revenue1.setAmount(BigDecimal.valueOf(100));
        Revenue revenue2 = new Revenue(); revenue2.setAmount(BigDecimal.valueOf(200));
        when(revenueRepository.findAllByUserAssignedId(user.getId())).thenReturn(List.of(revenue1, revenue2));
        when(expenseRepository.findAllByUserAssignedId(user.getId())).thenReturn(List.of());

        revenueScoringService.recalculateScore(EMAIL);

        assertEquals(BigDecimal.valueOf(3), revenueSettings.getRevenuePoints());
    }

    @Test
    void shouldAddAndSubtractPointsConsideringExpenses() {
        revenueSettings.setScoringEnable(true);

        Revenue revenue = new Revenue(); revenue.setAmount(BigDecimal.valueOf(50));
        Revenue revenue2 = new Revenue(); revenue2.setAmount(BigDecimal.valueOf(200));
        when(revenueRepository.findAllByUserAssignedId(user.getId())).thenReturn(List.of(revenue, revenue2));

        Expense expense = new Expense(); expense.setAmount(BigDecimal.valueOf(100));
        Expense expense2 = new Expense(); expense2.setAmount(BigDecimal.valueOf(150));
        when(expenseRepository.findAllByUserAssignedId(user.getId())).thenReturn(List.of(expense, expense2));

        revenueScoringService.recalculateScore(EMAIL);

        assertEquals(BigDecimal.valueOf(1), revenueSettings.getRevenuePoints());
    }

    @Test
    void shouldNotHaveNegativePoints() {
        revenueSettings.setScoringEnable(true);

        Revenue revenue = new Revenue(); revenue.setAmount(BigDecimal.valueOf(50));
        when(revenueRepository.findAllByUserAssignedId(user.getId())).thenReturn(List.of(revenue));

        Expense expense = new Expense(); expense.setAmount(BigDecimal.valueOf(100));
        when(expenseRepository.findAllByUserAssignedId(user.getId())).thenReturn(List.of(expense));

        revenueScoringService.recalculateScore(EMAIL);

        assertEquals(BigDecimal.ZERO, revenueSettings.getRevenuePoints());
    }
}