package com.finovara.finovarabackend.util.periodbalance.revenue;

import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialPeriodRevenueTest {

    @Mock
    private RevenueRepository revenueRepository;

    @InjectMocks
    private FinancialPeriodService financialPeriodService;

    private final Long USER_ID = 1L;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnRevenueInPeriod(PeriodType periodType) {
        LocalDate from = periodType.getStartDate(today);

        when(revenueRepository.sumRevenuesByUserAndDateRange(USER_ID, from, today))
                .thenReturn(Optional.of(BigDecimal.valueOf(100)));

        BigDecimal result = financialPeriodService.getRevenueSum(USER_ID, periodType);

        assertThat(result).isEqualByComparingTo("100");
        verify(revenueRepository).sumRevenuesByUserAndDateRange(USER_ID, from, today);
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnZeroWhenNoRevenue(PeriodType periodType) {
        LocalDate from = periodType.getStartDate(today);

        when(revenueRepository.sumRevenuesByUserAndDateRange(USER_ID, from, today)).thenReturn(Optional.empty());

        BigDecimal result = financialPeriodService.getRevenueSum(USER_ID, periodType);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnRevenueInPeriodByCategory(PeriodType periodType) {
        LocalDate from = periodType.getStartDate(today);

        financialPeriodService.getRevenuesInPeriodByCategory(USER_ID, periodType, RevenueCategory.SALARY);

        verify(revenueRepository).findAllByUserAssignedIdAndCreatedAtBetweenAndCategory(USER_ID, from, today, RevenueCategory.SALARY);
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnAverageRevenueInPeriod(PeriodType periodType) {
        LocalDate from = periodType.getStartDate(today);

        when(revenueRepository.avgRevenuesByUserAssignedIdAndPeriod(USER_ID, from, today))
                .thenReturn(Optional.of(BigDecimal.valueOf(200)));

        BigDecimal result = financialPeriodService.getAverageRevenue(USER_ID, periodType);

        assertThat(result).isEqualByComparingTo("200");
        verify(revenueRepository).avgRevenuesByUserAssignedIdAndPeriod(USER_ID, from, today);
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnZeroAverageRevenueWhenNoData(PeriodType periodType) {
        LocalDate from = periodType.getStartDate(today);

        when(revenueRepository.avgRevenuesByUserAssignedIdAndPeriod(USER_ID, from, today)).thenReturn(Optional.empty());

        BigDecimal result = financialPeriodService.getAverageRevenue(USER_ID, periodType);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }
}