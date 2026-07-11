package com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.revenue.service;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.reportservice.feignclient.FinanceBackendSharedReportClient;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.revenue.dto.SharedRevenueCategoryPercentageDto;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedRevenueCategoryPercentageServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;

    @Mock
    private FinanceBackendSharedReportClient reportClient;

    @Mock
    private RevenueCategory revenueCategory;

    @InjectMocks
    private SharedRevenueCategoryPercentageService sharedRevenueCategoryPercentageService;

    @Nested
    class GetRevenuePercentageByCategoryReport {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnCorrectPercentageForEveryPeriodType(PeriodType periodType) {
            LocalDate to = LocalDate.now();
            LocalDate from = periodType.getStartDate(to);
            when(reportClient.sumRevenues(OWNER_ID, MEMBER_ID, from, to)).thenReturn(new BigDecimal("200.00"));
            when(reportClient.revenuesByCategory(OWNER_ID, MEMBER_ID, from, to, revenueCategory)).thenReturn(new BigDecimal("50.00"));

            SharedRevenueCategoryPercentageDto result = sharedRevenueCategoryPercentageService
                    .getRevenuePercentageByCategoryReport(OWNER_ID, MEMBER_ID, revenueCategory, periodType);

            assertThat(result.percentage()).isEqualByComparingTo("25.00");
            assertThat(result.category()).isEqualTo(revenueCategory);
        }

        @Test
        void shouldReturnZeroPercentageWhenCategoryHasNoRevenue() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.MONTHLY.getStartDate(to);
            when(reportClient.sumRevenues(OWNER_ID, MEMBER_ID, from, to)).thenReturn(new BigDecimal("200.00"));
            when(reportClient.revenuesByCategory(OWNER_ID, MEMBER_ID, from, to, revenueCategory)).thenReturn(BigDecimal.ZERO);

            SharedRevenueCategoryPercentageDto result = sharedRevenueCategoryPercentageService
                    .getRevenuePercentageByCategoryReport(OWNER_ID, MEMBER_ID, revenueCategory, PeriodType.MONTHLY);

            assertThat(result.percentage()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldReturnFullPercentageWhenCategoryEqualsTotalRevenue() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.MONTHLY.getStartDate(to);
            when(reportClient.sumRevenues(OWNER_ID, MEMBER_ID, from, to)).thenReturn(new BigDecimal("100.00"));
            when(reportClient.revenuesByCategory(OWNER_ID, MEMBER_ID, from, to, revenueCategory)).thenReturn(new BigDecimal("100.00"));

            SharedRevenueCategoryPercentageDto result = sharedRevenueCategoryPercentageService
                    .getRevenuePercentageByCategoryReport(OWNER_ID, MEMBER_ID, revenueCategory, PeriodType.MONTHLY);

            assertThat(result.percentage()).isEqualByComparingTo("100.00");
        }

        @Test
        void shouldCallReportClientWithOwnerAndMemberIds() {
            LocalDate to = LocalDate.now();
            LocalDate from = PeriodType.WEEKLY.getStartDate(to);
            when(reportClient.sumRevenues(OWNER_ID, MEMBER_ID, from, to)).thenReturn(new BigDecimal("10.00"));
            when(reportClient.revenuesByCategory(OWNER_ID, MEMBER_ID, from, to, revenueCategory)).thenReturn(new BigDecimal("5.00"));

            sharedRevenueCategoryPercentageService.getRevenuePercentageByCategoryReport(OWNER_ID, MEMBER_ID, revenueCategory, PeriodType.WEEKLY);

            verify(reportClient).sumRevenues(OWNER_ID, MEMBER_ID, from, to);
            verify(reportClient).revenuesByCategory(OWNER_ID, MEMBER_ID, from, to, revenueCategory);
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            assertThrows(NullPointerException.class,
                    () -> sharedRevenueCategoryPercentageService.getRevenuePercentageByCategoryReport(OWNER_ID, MEMBER_ID, revenueCategory, null));
        }
    }
}