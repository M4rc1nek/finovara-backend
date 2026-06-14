package com.finovara.authbackend.limit.service;

import com.finovara.authbackend.limit.dto.LimitStatsDto;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.authbackend.limit.mapper.LimitMapper;
import com.finovara.authbackend.limit.model.Limit;
import com.finovara.authbackend.limit.model.LimitStatus;
import com.finovara.contracts.model.PeriodType;
import com.finovara.authbackend.limit.repository.LimitRepository;
import com.finovara.authbackend.util.periodbalance.FinancialPeriodService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LimitCalculateTest {
    @Mock
    private LimitRepository limitRepository;

    @Mock
    private FinancialPeriodService financialPeriodService;

    @Mock
    private LimitMapper limitMapper;

    @InjectMocks
    private LimitCalculateService limitService;

    @Test
    void shouldCalculateLimitStatsSuccessfully() {
        Long userId = 1L;
        Long limitId = 10L;
        LocalDate date = LocalDate.now();

        Limit limit = new Limit();
        limit.setPeriodType(PeriodType.DAILY);
        limit.setAmount(new BigDecimal("100"));

        BigDecimal spentToday = new BigDecimal("30");
        BigDecimal remaining = limit.getAmount().subtract(spentToday);
        BigDecimal percentage = new BigDecimal("30.00");
        LimitStatus status = LimitStatus.LOW;

        LimitStatsDto expectedDto = new LimitStatsDto(limitId, limit.getPeriodType(), limit.getAmount(), spentToday, remaining, percentage, status, date);

        when(limitRepository.findByIdAndUserId(userId, limitId)).thenReturn(Optional.of(limit));
        when(financialPeriodService.getExpensesSum(userId, PeriodType.DAILY)).thenReturn(spentToday);
        when(limitMapper.mapLimitStatsToDto(limit, spentToday, remaining, percentage, status, date)).thenReturn(expectedDto);

        LimitStatsDto result = limitService.calculateLimitStats(userId, limitId, date);

        assertEquals(result, expectedDto);
        verify(limitRepository).findByIdAndUserId(userId, limitId);
        verify(financialPeriodService).getExpensesSum(userId, PeriodType.DAILY);
        verify(limitMapper).mapLimitStatsToDto(limit, spentToday, remaining, percentage, status, date);
    }

    @Test
    void shouldThrowExceptionWhenLimitDoesNotExist() {
        Long userId = 1L;
        Long limitId = 10L;
        LocalDate date = LocalDate.now();

        when(limitRepository.findByIdAndUserId(userId, limitId)).thenReturn(Optional.empty());

        assertThrows(RequestedEntityNotFoundException.class, () -> limitService.calculateLimitStats(userId, limitId, date));

        verify(limitRepository).findByIdAndUserId(userId, limitId);
        verifyNoInteractions(financialPeriodService, limitMapper);
    }
}