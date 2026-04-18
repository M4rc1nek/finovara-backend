package com.finovara.finovarabackend.report.finances.service.categoryearned;

import com.finovara.finovarabackend.report.finances.categorypercentage.revenue.dto.RevenueCategoryPercentageDto;
import com.finovara.finovarabackend.report.finances.categorypercentage.revenue.service.RevenueCategoryPercentageService;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.periodbalance.FinancialPeriodService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueCategoryPercentageServiceTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private FinancialPeriodService financialPeriodService;

    @InjectMocks
    private RevenueCategoryPercentageService revenueCategoryPercentageService;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldGetCategoryEarnedReport(PeriodType periodType) {
        User user = new User();
        user.setId(1L);

        BigDecimal summedRevenue = BigDecimal.valueOf(100);

        Revenue revenue1 = new Revenue();
        revenue1.setAmount(BigDecimal.valueOf(20));

        Revenue revenue2 = new Revenue();
        revenue2.setAmount(BigDecimal.valueOf(30));

        List<Revenue> revenueCategory = List.of(revenue1, revenue2);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(financialPeriodService.getRevenueSum(user.getId(), periodType)).thenReturn(summedRevenue);
        when(financialPeriodService.getRevenuesInPeriodByCategory(user.getId(), periodType, RevenueCategory.SALARY))
                .thenReturn(revenueCategory);

        RevenueCategoryPercentageDto result = revenueCategoryPercentageService.getRevenuePercentageByCategoryReport(userId, RevenueCategory.SALARY, periodType);

        assertThat(result.percentage()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(result.category()).isEqualTo(RevenueCategory.SALARY);
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnZeroPercentageWhenRevenueIsZero(PeriodType periodType) {
        User user = new User();
        user.setId(1L);

        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(financialPeriodService.getRevenueSum(user.getId(), periodType)).thenReturn(BigDecimal.ZERO);

        when(financialPeriodService.getRevenuesInPeriodByCategory(user.getId(), periodType, RevenueCategory.SALARY))
                .thenReturn(List.of());

        RevenueCategoryPercentageDto result = revenueCategoryPercentageService.getRevenuePercentageByCategoryReport(userId, RevenueCategory.SALARY, periodType);

        assertThat(result.percentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {

        when(userManagerService.getUserByIdOrThrow(userId)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () ->
                revenueCategoryPercentageService.getRevenuePercentageByCategoryReport(userId, RevenueCategory.BONUS, PeriodType.WEEKLY));
    }
}