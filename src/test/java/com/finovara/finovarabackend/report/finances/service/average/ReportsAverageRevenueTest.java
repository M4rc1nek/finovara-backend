package com.finovara.finovarabackend.report.finances.service.average;

import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.report.dto.ReportDto;
import com.finovara.finovarabackend.report.finances.average.service.ReportAverageService;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportsAverageRevenueTest {

    @Mock
    private FinancialPeriodService financialPeriodService;

    @Mock
    private RevenueRepository revenueRepository;

    @InjectMocks
    private ReportAverageService reportAverageService;


    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldCalculateAverageRevenue(PeriodType periodType){
        Long userId = 1L;
        List<Revenue> revenues = List.of(new Revenue(), new Revenue());

        when(revenueRepository.findAllByUserAssignedId(userId)).thenReturn(revenues);
        when(financialPeriodService.getEarned(userId, periodType)).thenReturn(BigDecimal.valueOf(200));

        ReportDto result = reportAverageService.calculateAverageRevenue(userId, periodType);

        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(100));

        assertThat(result.periodType()).isEqualTo(periodType);
    }

    @Test
    void shouldReturnZeroWhenNoRevenues() {
        Long userId = 1L;

        when(revenueRepository.findAllByUserAssignedId(userId)).thenReturn(List.of());

        when(financialPeriodService.getEarned(userId, PeriodType.DAILY)).thenReturn(BigDecimal.ZERO);

        ReportDto result = reportAverageService.calculateAverageRevenue(userId, PeriodType.DAILY);

        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(result.periodType()).isEqualTo(PeriodType.DAILY);
    }

}