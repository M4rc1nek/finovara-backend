package com.finovara.finovarabackend.revenuehistory.service;

import com.finovara.finovarabackend.revenue.dto.RevenueDTO;
import com.finovara.finovarabackend.revenue.mapper.RevenueMapper;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
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
    private String email;
    private Revenue revenue;
    private RevenueDTO revenueDTO;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        email = "test@email.com";

        revenue = new Revenue();
        revenueDTO = new RevenueDTO(null, null, new BigDecimal(200),
                RevenueCategory.SALARY, LocalDate.of(2026, 3, 12), "test"
        );
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnMappedRevenuesForEachPeriod(PeriodType periodType) {
        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(financialPeriodService.getRevenuesInPeriodByCategory(1L, periodType, RevenueCategory.SALARY)).thenReturn(List.of(revenue));
        when(revenueMapper.mapRevenueToDTO(revenue)).thenReturn(revenueDTO);

        List<RevenueDTO> result = revenueHistoryService.getRevenueByCategory(email, periodType, RevenueCategory.SALARY);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(revenueDTO);

        verify(userManagerService).getUserByEmailOrThrow(email);
        verify(financialPeriodService).getRevenuesInPeriodByCategory(1L, periodType, RevenueCategory.SALARY);
        verify(revenueMapper).mapRevenueToDTO(revenue);
    }

    @Test
    void shouldReturnEmptyListWhenNoRevenues() {
        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);
        when(financialPeriodService.getRevenuesInPeriodByCategory(1L, PeriodType.DAILY, RevenueCategory.SALARY)).thenReturn(List.of());

        List<RevenueDTO> result = revenueHistoryService.getRevenueByCategory(email, PeriodType.DAILY, RevenueCategory.SALARY);

        assertThat(result).isEmpty();

        verify(revenueMapper, never()).mapRevenueToDTO(any());
    }
}