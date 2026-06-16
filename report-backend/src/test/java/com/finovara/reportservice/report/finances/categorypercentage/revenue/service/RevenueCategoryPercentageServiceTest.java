package com.finovara.reportservice.report.finances.categorypercentage.revenue.service;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.report.finances.categorypercentage.revenue.dto.RevenueCategoryPercentageDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueCategoryPercentageServiceTest {

    private static final Long USER_ID = 1L;
    private static final RevenueCategory CATEGORY = RevenueCategory.SALARY;

    @Mock
    private FinanceBackendReportClient reportClient;

    @InjectMocks
    private RevenueCategoryPercentageService revenueCategoryPercentageService;

    @Nested
    class GetRevenuePercentageByCategoryReport {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldDelegateToClientAndReturnPercentage(PeriodType periodType) {
            LocalDate to = LocalDate.now();
            LocalDate from = periodType.getStartDate(to);

            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.revenuesByCategory(USER_ID, from, to, CATEGORY)).thenReturn(BigDecimal.valueOf(50));

            RevenueCategoryPercentageDto result = revenueCategoryPercentageService
                    .getRevenuePercentageByCategoryReport(USER_ID, CATEGORY, periodType);

            assertThat(result.percentage()).isEqualByComparingTo("50");
            assertThat(result.category()).isEqualTo(CATEGORY);
            verify(reportClient).sumRevenues(USER_ID, from, to);
            verify(reportClient).revenuesByCategory(USER_ID, from, to, CATEGORY);
        }

        @Test
        void shouldReturnZeroPercentageWhenTotalOrCategoryAmountIsZero() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.MONTHLY.getStartDate(to);

            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.revenuesByCategory(USER_ID, from, to, CATEGORY)).thenReturn(BigDecimal.ZERO);

            RevenueCategoryPercentageDto result = revenueCategoryPercentageService
                    .getRevenuePercentageByCategoryReport(USER_ID, CATEGORY, PeriodType.MONTHLY);

            assertThat(result.percentage()).isEqualByComparingTo("0");
        }

        @Test
        void shouldReturnOneHundredPercentWhenAllRevenuesAreInCategory() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.MONTHLY.getStartDate(to);

            when(reportClient.sumRevenues(USER_ID, from, to)).thenReturn(BigDecimal.valueOf(100));
            when(reportClient.revenuesByCategory(USER_ID, from, to, CATEGORY)).thenReturn(BigDecimal.valueOf(100));

            RevenueCategoryPercentageDto result = revenueCategoryPercentageService
                    .getRevenuePercentageByCategoryReport(USER_ID, CATEGORY, PeriodType.MONTHLY);

            assertThat(result.percentage()).isEqualByComparingTo("100");
        }
    }
}
