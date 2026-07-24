package com.finovara.financeservice.limit.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.financeservice.limit.dto.LimitStatsDto;
import com.finovara.financeservice.limit.mapper.LimitMapper;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.model.LimitStatus;
import com.finovara.financeservice.limit.repository.LimitRepository;
import com.finovara.financeservice.util.periodbalance.FinancialPeriodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LimitCalculateServiceTest {

    @Mock
    private FinancialPeriodService financialPeriodService;

    @Mock
    private LimitRepository limitRepository;

    @Mock
    private LimitMapper limitMapper;

    private LimitCalculateService limitCalculateService;

    private Long userId;

    private LocalDate date;

    private LimitStatsDto expectedDto;

    @BeforeEach
    void setUp() {
        limitCalculateService = new LimitCalculateService(financialPeriodService, limitRepository, limitMapper);
        userId = 1L;
        date = LocalDate.of(2026, 7, 15);
        expectedDto = mock(LimitStatsDto.class);
    }

    private Limit buildLimit(Long id, BigDecimal amount, PeriodType periodType, ExpenseCategory category) {
        return Limit.builder()
                .id(id)
                .amount(amount)
                .periodType(periodType)
                .category(category)
                .isActive(true)
                .userId(userId)
                .build();
    }

    @Nested
    class CalculateLimitStatsWithLimitObject {

        @Test
        void shouldReturnStatsWithHighStatusWhenPercentageIsEightyOrMore() {
            Limit limit = buildLimit(1L, BigDecimal.valueOf(200), PeriodType.MONTHLY, null);
            when(financialPeriodService.getExpensesSum(userId, limit.getPeriodType(), limit.getCategory()))
                    .thenReturn(BigDecimal.valueOf(160));
            when(limitMapper.mapLimitStatsToDto(eq(limit), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), eq(LimitStatus.HIGH), eq(date)))
                    .thenReturn(expectedDto);

            LimitStatsDto result = limitCalculateService.calculateLimitStats(limit, userId, date);

            assertSame(expectedDto, result);
        }

        @Test
        void shouldReturnStatsWithMediumStatusWhenPercentageIsFiftyOrMore() {
            Limit limit = buildLimit(1L, BigDecimal.valueOf(200), PeriodType.MONTHLY, null);
            when(financialPeriodService.getExpensesSum(userId, limit.getPeriodType(), limit.getCategory()))
                    .thenReturn(BigDecimal.valueOf(100));
            when(limitMapper.mapLimitStatsToDto(eq(limit), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), eq(LimitStatus.MEDIUM), eq(date)))
                    .thenReturn(expectedDto);

            LimitStatsDto result = limitCalculateService.calculateLimitStats(limit, userId, date);

            assertSame(expectedDto, result);
        }

        @Test
        void shouldReturnStatsWithLowStatusWhenPercentageIsTwentyFiveOrMore() {
            Limit limit = buildLimit(1L, BigDecimal.valueOf(200), PeriodType.MONTHLY, null);
            when(financialPeriodService.getExpensesSum(userId, limit.getPeriodType(), limit.getCategory()))
                    .thenReturn(BigDecimal.valueOf(50));
            when(limitMapper.mapLimitStatsToDto(eq(limit), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), eq(LimitStatus.LOW), eq(date)))
                    .thenReturn(expectedDto);

            LimitStatsDto result = limitCalculateService.calculateLimitStats(limit, userId, date);

            assertSame(expectedDto, result);
        }

        @Test
        void shouldReturnStatsWithNoneStatusWhenPercentageBelowTwentyFive() {
            Limit limit = buildLimit(1L, BigDecimal.valueOf(200), PeriodType.MONTHLY, null);
            when(financialPeriodService.getExpensesSum(userId, limit.getPeriodType(), limit.getCategory()))
                    .thenReturn(BigDecimal.valueOf(20));
            when(limitMapper.mapLimitStatsToDto(eq(limit), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), eq(LimitStatus.NONE), eq(date)))
                    .thenReturn(expectedDto);

            LimitStatsDto result = limitCalculateService.calculateLimitStats(limit, userId, date);

            assertSame(expectedDto, result);
        }

        @Test
        void shouldClampRemainingToZeroWhenSpentExceedsAmount() {
            Limit limit = buildLimit(1L, BigDecimal.valueOf(200), PeriodType.MONTHLY, null);
            when(financialPeriodService.getExpensesSum(userId, limit.getPeriodType(), limit.getCategory()))
                    .thenReturn(BigDecimal.valueOf(250));
            when(limitMapper.mapLimitStatsToDto(eq(limit), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), eq(LimitStatus.HIGH), eq(date)))
                    .thenReturn(expectedDto);

            limitCalculateService.calculateLimitStats(limit, userId, date);

            ArgumentCaptor<BigDecimal> remainingCaptor = ArgumentCaptor.forClass(BigDecimal.class);
            verify(limitMapper).mapLimitStatsToDto(eq(limit), any(BigDecimal.class), remainingCaptor.capture(), any(BigDecimal.class), eq(LimitStatus.HIGH), eq(date));
            assertEquals(0, remainingCaptor.getValue().compareTo(BigDecimal.ZERO));
        }

        @Nested
        class CalculateLimitStatsWithLimitId {

            @Test
            void shouldReturnStatsWhenLimitExistsForUser() {
                Limit limit = buildLimit(5L, BigDecimal.valueOf(300), PeriodType.MONTHLY, null);
                when(limitRepository.findByIdAndUserId(userId, limit.getId())).thenReturn(Optional.of(limit));
                when(financialPeriodService.getExpensesSum(userId, limit.getPeriodType(), limit.getCategory()))
                        .thenReturn(BigDecimal.valueOf(30));
                when(limitMapper.mapLimitStatsToDto(eq(limit), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), eq(LimitStatus.NONE), eq(date)))
                        .thenReturn(expectedDto);

                LimitStatsDto result = limitCalculateService.calculateLimitStats(userId, limit.getId(), date);

                assertSame(expectedDto, result);
            }

            @Test
            void shouldThrowExceptionWhenLimitNotFoundForUser() {
                when(limitRepository.findByIdAndUserId(userId, 99L)).thenReturn(Optional.empty());

                assertThrows(RequestedEntityNotFoundException.class,
                        () -> limitCalculateService.calculateLimitStats(userId, 99L, date));
            }
        }
    }
}