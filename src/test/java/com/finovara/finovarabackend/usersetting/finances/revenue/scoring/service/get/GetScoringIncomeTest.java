package com.finovara.finovarabackend.usersetting.finances.revenue.scoring.service.get;

import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.revenue.model.RevenueSettings;
import com.finovara.finovarabackend.usersetting.finances.revenue.scoring.dto.RevenueScoringDto;
import com.finovara.finovarabackend.usersetting.finances.revenue.scoring.service.RevenueScoringService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetScoringIncomeTest {

    @Mock
    private UserManagerService userManagerService;

    @InjectMocks
    private RevenueScoringService revenueScoringService;

    private RevenueSettings revenueSettings;
    private final String EMAIL = "test@test.com";

    @BeforeEach
    void setup() {
        User user = new User();
        revenueSettings = new RevenueSettings();
        user.setRevenueSettings(revenueSettings);

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
    }

    @Test
    void shouldReturnScoringIncomeWhenEnabled() {
        revenueSettings.setScoringEnable(true);
        revenueSettings.setRevenuePoints(BigDecimal.valueOf(5));

        RevenueScoringDto dto = revenueScoringService.getScoringIncome(EMAIL);

        assertEquals(true, dto.scoringEnable());
        assertEquals(BigDecimal.valueOf(5), dto.revenuePoints());
    }

    @Test
    void shouldReturnScoringIncomeWhenDisabled() {
        revenueSettings.setScoringEnable(false);
        revenueSettings.setRevenuePoints(BigDecimal.ZERO);

        RevenueScoringDto dto = revenueScoringService.getScoringIncome(EMAIL);

        assertEquals(false, dto.scoringEnable());
        assertEquals(BigDecimal.ZERO, dto.revenuePoints());
    }
}