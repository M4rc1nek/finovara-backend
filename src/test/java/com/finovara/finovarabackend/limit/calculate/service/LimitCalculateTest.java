package com.finovara.finovarabackend.limit.calculate.service;

import com.finovara.finovarabackend.limit.dto.LimitStatsDto;
import com.finovara.finovarabackend.limit.exception.notfound.ActiveLimitNotFoundException;
import com.finovara.finovarabackend.limit.mapper.LimitMapper;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.limit.model.LimitStatus;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.limit.repository.LimitRepository;
import com.finovara.finovarabackend.limit.service.LimitCalculateService;
import com.finovara.finovarabackend.util.periodbalance.FinancialPeriodService;
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

        when(limitRepository.findByIdAndUserAssignedId(userId, limitId)).thenReturn(Optional.of(limit));
        when(financialPeriodService.getExpensesSum(userId, PeriodType.DAILY)).thenReturn(spentToday);
        when(limitMapper.mapLimitStatsToDto(limit, spentToday, remaining, percentage, status, date)).thenReturn(expectedDto);

        LimitStatsDto result = limitService.calculateLimitStats(userId, limitId, date);

        assertEquals(result, expectedDto);
        verify(limitRepository).findByIdAndUserAssignedId(userId, limitId);
        verify(financialPeriodService).getExpensesSum(userId, PeriodType.DAILY);
        verify(limitMapper).mapLimitStatsToDto(limit, spentToday, remaining, percentage, status, date);
    }

    @Test
    void shouldThrowExceptionWhenLimitDoesNotExist() {
        Long userId = 1L;
        Long limitId = 10L;
        LocalDate date = LocalDate.now();

        when(limitRepository.findByIdAndUserAssignedId(userId, limitId)).thenReturn(Optional.empty());

        assertThrows(ActiveLimitNotFoundException.class, () -> limitService.calculateLimitStats(userId, limitId, date));

        verify(limitRepository).findByIdAndUserAssignedId(userId, limitId);
        verifyNoInteractions(financialPeriodService, limitMapper);
    }
}