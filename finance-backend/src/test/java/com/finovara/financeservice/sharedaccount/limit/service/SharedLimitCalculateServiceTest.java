package com.finovara.financeservice.sharedaccount.limit.service;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.financeservice.limit.model.LimitStatus;
import com.finovara.financeservice.sharedaccount.limit.dto.SharedLimitStatsDto;
import com.finovara.financeservice.sharedaccount.limit.mapper.SharedLimitMapper;
import com.finovara.financeservice.sharedaccount.limit.model.SharedLimit;
import com.finovara.financeservice.sharedaccount.limit.repository.SharedLimitRepository;
import com.finovara.financeservice.util.periodbalance.FinancialPeriodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedLimitCalculateServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long LIMIT_ID = 10L;

    @Mock
    private FinancialPeriodService financialPeriodService;

    @Mock
    private SharedLimitRepository limitRepository;

    @Mock
    private SharedLimitMapper limitMapper;

    @InjectMocks
    private SharedLimitCalculateService sharedLimitCalculateService;

    private SharedLimit limit;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        limit = SharedLimit.builder()
                .id(LIMIT_ID)
                .periodType(PeriodType.MONTHLY)
                .category(ExpenseCategory.FOOD)
                .amount(BigDecimal.valueOf(500))
                .isActive(true)
                .build();

        today = LocalDate.now();
    }

    @Nested
    class CalculateLimitStatsWithLimitObject {

        @Test
        void shouldReturnStatsWithRemainingWhenSpentIsLessThanLimit() {
            when(financialPeriodService.getSharedExpensesSum(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(BigDecimal.valueOf(200));

            SharedLimitStatsDto expectedDto = mock(SharedLimitStatsDto.class);
            when(limitMapper.mapLimitStatsToDto(eq(limit), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), eq(LimitStatus.LOW), eq(today)))
                    .thenReturn(expectedDto);

            SharedLimitStatsDto result = sharedLimitCalculateService.calculateLimitStats(limit, USER_ID, today);

            assertEquals(expectedDto, result);

            ArgumentCaptor<BigDecimal> spentCaptor = ArgumentCaptor.forClass(BigDecimal.class);
            ArgumentCaptor<BigDecimal> remainingCaptor = ArgumentCaptor.forClass(BigDecimal.class);
            verify(limitMapper).mapLimitStatsToDto(eq(limit), spentCaptor.capture(), remainingCaptor.capture(), any(BigDecimal.class), eq(LimitStatus.LOW), eq(today));
            assertEquals(0, spentCaptor.getValue().compareTo(BigDecimal.valueOf(200)));
            assertEquals(0, remainingCaptor.getValue().compareTo(BigDecimal.valueOf(300)));
        }

        @Test
        void shouldClampRemainingToZeroWhenSpentExceedsLimit() {
            when(financialPeriodService.getSharedExpensesSum(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(BigDecimal.valueOf(700));

            SharedLimitStatsDto expectedDto = mock(SharedLimitStatsDto.class);
            when(limitMapper.mapLimitStatsToDto(eq(limit), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), eq(LimitStatus.HIGH), eq(today)))
                    .thenReturn(expectedDto);

            SharedLimitStatsDto result = sharedLimitCalculateService.calculateLimitStats(limit, USER_ID, today);

            assertEquals(expectedDto, result);

            ArgumentCaptor<BigDecimal> remainingCaptor = ArgumentCaptor.forClass(BigDecimal.class);
            verify(limitMapper).mapLimitStatsToDto(eq(limit), any(BigDecimal.class), remainingCaptor.capture(), any(BigDecimal.class), eq(LimitStatus.HIGH), eq(today));
            assertEquals(0, remainingCaptor.getValue().compareTo(BigDecimal.ZERO));
        }

        @Test
        void shouldReturnHighStatusWhenPercentageIsAtLeast80() {
            when(financialPeriodService.getSharedExpensesSum(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(BigDecimal.valueOf(400));

            sharedLimitCalculateService.calculateLimitStats(limit, USER_ID, today);

            verify(limitMapper).mapLimitStatsToDto(eq(limit), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), eq(LimitStatus.HIGH), eq(today));
        }

        @Test
        void shouldReturnMediumStatusWhenPercentageIsAtLeast50AndBelow80() {
            when(financialPeriodService.getSharedExpensesSum(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(BigDecimal.valueOf(250));

            sharedLimitCalculateService.calculateLimitStats(limit, USER_ID, today);

            verify(limitMapper).mapLimitStatsToDto(eq(limit), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), eq(LimitStatus.MEDIUM), eq(today));
        }

        @Test
        void shouldReturnLowStatusWhenPercentageIsAtLeast25AndBelow50() {
            when(financialPeriodService.getSharedExpensesSum(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(BigDecimal.valueOf(125));

            sharedLimitCalculateService.calculateLimitStats(limit, USER_ID, today);

            verify(limitMapper).mapLimitStatsToDto(eq(limit), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), eq(LimitStatus.LOW), eq(today));
        }

        @Test
        void shouldReturnNoneStatusWhenPercentageIsBelow25() {
            when(financialPeriodService.getSharedExpensesSum(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(BigDecimal.valueOf(50));

            sharedLimitCalculateService.calculateLimitStats(limit, USER_ID, today);

            verify(limitMapper).mapLimitStatsToDto(eq(limit), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), eq(LimitStatus.NONE), eq(today));
        }

        @Test
        void shouldReturnNoneStatusWhenNothingSpent() {
            when(financialPeriodService.getSharedExpensesSum(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(BigDecimal.ZERO);

            sharedLimitCalculateService.calculateLimitStats(limit, USER_ID, today);

            ArgumentCaptor<BigDecimal> spentCaptor = ArgumentCaptor.forClass(BigDecimal.class);
            ArgumentCaptor<BigDecimal> remainingCaptor = ArgumentCaptor.forClass(BigDecimal.class);
            verify(limitMapper).mapLimitStatsToDto(eq(limit), spentCaptor.capture(), remainingCaptor.capture(), any(BigDecimal.class), eq(LimitStatus.NONE), eq(today));
            assertEquals(0, spentCaptor.getValue().compareTo(BigDecimal.ZERO));
            assertEquals(0, remainingCaptor.getValue().compareTo(BigDecimal.valueOf(500)));
        }
    }

    @Nested
    class CalculateLimitStatsWithLimitId {

        @Test
        void shouldReturnStatsWhenLimitExistsForUser() {
            when(limitRepository.findByIdAndUserId(USER_ID, LIMIT_ID)).thenReturn(Optional.of(limit));
            when(financialPeriodService.getSharedExpensesSum(USER_ID, PeriodType.MONTHLY, ExpenseCategory.FOOD)).thenReturn(BigDecimal.valueOf(100));

            SharedLimitStatsDto expectedDto = mock(SharedLimitStatsDto.class);
            when(limitMapper.mapLimitStatsToDto(eq(limit), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), any(LimitStatus.class), eq(today)))
                    .thenReturn(expectedDto);

            SharedLimitStatsDto result = sharedLimitCalculateService.calculateLimitStats(USER_ID, LIMIT_ID, today);

            assertEquals(expectedDto, result);
        }

        @Test
        void shouldThrowRequestedEntityNotFoundExceptionWhenLimitDoesNotExist() {
            when(limitRepository.findByIdAndUserId(USER_ID, LIMIT_ID)).thenReturn(Optional.empty());

            assertThrows(RequestedEntityNotFoundException.class, () -> sharedLimitCalculateService.calculateLimitStats(USER_ID, LIMIT_ID, today));

            verify(financialPeriodService, never()).getSharedExpensesSum(any(), any(), any());
        }
    }
}