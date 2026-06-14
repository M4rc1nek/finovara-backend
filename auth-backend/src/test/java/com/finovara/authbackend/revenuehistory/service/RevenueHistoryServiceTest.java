package com.finovara.authbackend.revenuehistory.service;

import com.finovara.authbackend.revenue.dto.RevenueDto;
import com.finovara.authbackend.revenue.mapper.RevenueMapper;
import com.finovara.authbackend.revenue.model.Revenue;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.authbackend.user.model.User;
import com.finovara.contracts.model.PeriodType;
import com.finovara.authbackend.util.periodbalance.FinancialPeriodService;
import com.finovara.authbackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevenueHistoryServiceTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private FinancialPeriodService financialPeriodService;

    @Mock
    private RevenueMapper revenueMapper;

    @InjectMocks
    private RevenueHistoryService revenueHistoryService;

    private User user;
    private Long userId;
    private Revenue revenue;
    private RevenueDto revenueDto;

    @BeforeEach
    void setUp() {
        user = new User();
        userId = 1L;
        user.setId(userId);

        revenue = new Revenue();
        revenueDto = new RevenueDto(null, null, new BigDecimal(200),
                RevenueCategory.SALARY, LocalDate.of(2026, 3, 12), "test"
        );
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnMappedRevenuesForEachPeriod(PeriodType periodType) {
        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(financialPeriodService.getRevenuesInPeriodByCategory(1L, periodType, RevenueCategory.SALARY)).thenReturn(List.of(revenue));
        when(revenueMapper.mapRevenueToDto(revenue)).thenReturn(revenueDto);

        List<RevenueDto> result = revenueHistoryService.getRevenueByCategory(userId, periodType, RevenueCategory.SALARY);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(revenueDto);

        verify(userManagerService).getUserByIdOrThrow(userId);
        verify(financialPeriodService).getRevenuesInPeriodByCategory(1L, periodType, RevenueCategory.SALARY);
        verify(revenueMapper).mapRevenueToDto(revenue);
    }

    @Test
    void shouldReturnEmptyListWhenNoRevenues() {
        when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
        when(financialPeriodService.getRevenuesInPeriodByCategory(1L, PeriodType.DAILY, RevenueCategory.SALARY)).thenReturn(List.of());

        List<RevenueDto> result = revenueHistoryService.getRevenueByCategory(userId, PeriodType.DAILY, RevenueCategory.SALARY);

        assertThat(result).isEmpty();

        verify(revenueMapper, never()).mapRevenueToDto(any());
    }
}