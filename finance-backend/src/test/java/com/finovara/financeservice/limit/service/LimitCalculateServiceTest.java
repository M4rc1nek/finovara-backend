package com.finovara.financeservice.limit.service;

import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.calculate.CalculatePercentage;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.limit.dto.LimitStatsDto;
import com.finovara.financeservice.limit.mapper.LimitMapper;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.model.LimitStatus;
import com.finovara.financeservice.util.limit.manager.LimitManagerService;
import com.finovara.financeservice.util.periodbalance.FinancialPeriodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LimitCalculateServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long LIMIT_ID = 10L;
    private static final Long OTHER_LIMIT_ID = 11L;
    private static final LocalDate DATE = LocalDate.of(2026, 10, 15);
    private static final PeriodType PERIOD = PeriodType.values()[0];
    private static final ExpenseCategory CATEGORY = ExpenseCategory.values()[0];
    private static final BigDecimal LIMIT_AMOUNT = BigDecimal.valueOf(100);

    @Mock
    private FinancialPeriodService financialPeriodService;

    @Mock
    private LimitManagerService limitManagerService;

    @Mock
    private LimitSpendingService limitSpendingService;

    @Mock
    private LimitMapper limitMapper;

    @Mock
    private LimitStatsDto statsDto;

    @Mock
    private LimitStatsDto otherStatsDto;

    @Captor
    private ArgumentCaptor<BigDecimal> remainingCaptor;

    @Captor
    private ArgumentCaptor<BigDecimal> percentageCaptor;

    @Captor
    private ArgumentCaptor<LimitStatus> statusCaptor;

    @InjectMocks
    private LimitCalculateService service;

    private Limit categoryLimit;
    private Limit generalLimit;

    @BeforeEach
    void setUp() {
        categoryLimit = Limit.builder().id(LIMIT_ID).periodType(PERIOD).category(CATEGORY).amount(LIMIT_AMOUNT).build();
        generalLimit = Limit.builder().id(OTHER_LIMIT_ID).periodType(PERIOD).category(null).amount(LIMIT_AMOUNT).build();
    }

    @Nested
    class CalculateLimitStatsForLimit {

        @Test
        void shouldReturnStatsWhenCategoryLimitProvided() {
            BigDecimal spent = BigDecimal.valueOf(40);
            when(financialPeriodService.getExpensesSum(USER_ID, PERIOD, CATEGORY)).thenReturn(spent);
            when(limitMapper.mapLimitStatsToDto(eq(categoryLimit), eq(spent), eq(BigDecimal.valueOf(60)), any(BigDecimal.class), any(LimitStatus.class), eq(DATE)))
                    .thenReturn(statsDto);

            LimitStatsDto result = service.calculateLimitStats(categoryLimit, USER_ID, DATE);

            assertSame(statsDto, result);
            verify(financialPeriodService).getExpensesSum(USER_ID, PERIOD, CATEGORY);
        }

        @Test
        void shouldRequestSumWithoutCategoryWhenGeneralLimitProvided() {
            BigDecimal spent = BigDecimal.valueOf(70);
            when(financialPeriodService.getExpensesSum(USER_ID, PERIOD, null)).thenReturn(spent);
            when(limitMapper.mapLimitStatsToDto(eq(generalLimit), eq(spent), eq(BigDecimal.valueOf(30)), any(BigDecimal.class), any(LimitStatus.class), eq(DATE)))
                    .thenReturn(statsDto);

            LimitStatsDto result = service.calculateLimitStats(generalLimit, USER_ID, DATE);

            assertSame(statsDto, result);
            verify(financialPeriodService).getExpensesSum(USER_ID, PERIOD, null);
        }

        @Test
        void shouldThrowExceptionWhenSumIsNull() {
            when(financialPeriodService.getExpensesSum(USER_ID, PERIOD, CATEGORY)).thenReturn(null);

            assertThrows(NullPointerException.class, () -> service.calculateLimitStats(categoryLimit, USER_ID, DATE));
            verifyNoInteractions(limitMapper);
        }
    }

    @Nested
    class CalculateLimitStatsById {

        @Test
        void shouldReturnStatsWhenLimitExists() {
            BigDecimal spent = BigDecimal.valueOf(25);
            when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID)).thenReturn(categoryLimit);
            when(financialPeriodService.getExpensesSum(USER_ID, PERIOD, CATEGORY)).thenReturn(spent);
            when(limitMapper.mapLimitStatsToDto(eq(categoryLimit), eq(spent), eq(BigDecimal.valueOf(75)), any(BigDecimal.class), any(LimitStatus.class), eq(DATE)))
                    .thenReturn(statsDto);

            LimitStatsDto result = service.calculateLimitStats(USER_ID, LIMIT_ID, DATE);

            assertSame(statsDto, result);
            verify(limitManagerService).getLimitByUserIdOrThrow(USER_ID, LIMIT_ID);
        }

        @Test
        void shouldThrowExceptionWhenLimitDoesNotExist() {
            when(limitManagerService.getLimitByUserIdOrThrow(USER_ID, LIMIT_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> service.calculateLimitStats(USER_ID, LIMIT_ID, DATE));
            verifyNoInteractions(financialPeriodService, limitMapper);
        }
    }

    @Nested
    class CalculateLimitStatsForList {

        @Test
        void shouldReturnStatsForEveryLimitWhenSpentCalculated() {
            List<Limit> limits = List.of(categoryLimit, generalLimit);
            BigDecimal firstSpent = BigDecimal.valueOf(10);
            BigDecimal secondSpent = BigDecimal.valueOf(90);
            when(limitSpendingService.calculateSpent(USER_ID, limits, DATE))
                    .thenReturn(Map.of(LIMIT_ID, firstSpent, OTHER_LIMIT_ID, secondSpent));
            when(limitMapper.mapLimitStatsToDto(eq(categoryLimit), eq(firstSpent), eq(BigDecimal.valueOf(90)), any(BigDecimal.class), any(LimitStatus.class), eq(DATE)))
                    .thenReturn(statsDto);
            when(limitMapper.mapLimitStatsToDto(eq(generalLimit), eq(secondSpent), eq(BigDecimal.valueOf(10)), any(BigDecimal.class), any(LimitStatus.class), eq(DATE)))
                    .thenReturn(otherStatsDto);

            List<LimitStatsDto> result = service.calculateLimitStats(limits, USER_ID, DATE);

            assertEquals(List.of(statsDto, otherStatsDto), result);
            verify(limitSpendingService).calculateSpent(USER_ID, limits, DATE);
        }

        @Test
        void shouldReturnEmptyListWhenNoLimitsProvided() {
            List<Limit> limits = List.of();
            when(limitSpendingService.calculateSpent(USER_ID, limits, DATE)).thenReturn(Map.of());

            List<LimitStatsDto> result = service.calculateLimitStats(limits, USER_ID, DATE);

            assertTrue(result.isEmpty());
            verifyNoInteractions(limitMapper);
        }

        @Test
        void shouldThrowExceptionWhenSpentMissingForLimit() {
            List<Limit> limits = List.of(categoryLimit);
            when(limitSpendingService.calculateSpent(USER_ID, limits, DATE)).thenReturn(Map.of());

            assertThrows(NullPointerException.class, () -> service.calculateLimitStats(limits, USER_ID, DATE));
            verifyNoInteractions(limitMapper);
        }
    }

    @Nested
    class BuildLimitStats {

        @Test
        void shouldReturnMappedDtoWhenSpentBelowLimit() {
            BigDecimal spent = BigDecimal.valueOf(40);
            when(limitMapper.mapLimitStatsToDto(eq(categoryLimit), eq(spent), eq(BigDecimal.valueOf(60)), any(BigDecimal.class), any(LimitStatus.class), eq(DATE)))
                    .thenReturn(statsDto);

            LimitStatsDto result = service.buildLimitStats(categoryLimit, spent, DATE);

            assertSame(statsDto, result);
        }

        @Test
        void shouldReturnRemainingEqualToLimitWhenNothingSpent() {
            service.buildLimitStats(categoryLimit, BigDecimal.ZERO, DATE);

            verify(limitMapper).mapLimitStatsToDto(eq(categoryLimit), eq(BigDecimal.ZERO), remainingCaptor.capture(), any(BigDecimal.class), any(LimitStatus.class), eq(DATE));
            assertEquals(0, LIMIT_AMOUNT.compareTo(remainingCaptor.getValue()));
        }

        @Test
        void shouldReturnZeroRemainingWhenSpentEqualsLimit() {
            service.buildLimitStats(categoryLimit, LIMIT_AMOUNT, DATE);

            verify(limitMapper).mapLimitStatsToDto(eq(categoryLimit), eq(LIMIT_AMOUNT), remainingCaptor.capture(), any(BigDecimal.class), any(LimitStatus.class), eq(DATE));
            assertEquals(0, BigDecimal.ZERO.compareTo(remainingCaptor.getValue()));
        }

        @Test
        void shouldReturnZeroRemainingWhenSpentExceedsLimit() {
            BigDecimal spent = BigDecimal.valueOf(150);

            service.buildLimitStats(categoryLimit, spent, DATE);

            verify(limitMapper).mapLimitStatsToDto(eq(categoryLimit), eq(spent), remainingCaptor.capture(), any(BigDecimal.class), any(LimitStatus.class), eq(DATE));
            assertEquals(0, BigDecimal.ZERO.compareTo(remainingCaptor.getValue()));
        }

        @Test
        void shouldPassPercentageAndStatusDerivedFromSpentWhenBuildingStats() {
            BigDecimal spent = BigDecimal.valueOf(80);

            service.buildLimitStats(categoryLimit, spent, DATE);

            verify(limitMapper).mapLimitStatsToDto(eq(categoryLimit), eq(spent), any(BigDecimal.class), percentageCaptor.capture(), statusCaptor.capture(), eq(DATE));
            assertEquals(CalculatePercentage.calculatePercentage(spent, LIMIT_AMOUNT), percentageCaptor.getValue());
            assertEquals(LimitStatus.fromPercentage(percentageCaptor.getValue()), statusCaptor.getValue());
        }

        @Test
        void shouldPassStatusDerivedFromPercentageWhenSpentExceedsLimit() {
            BigDecimal spent = BigDecimal.valueOf(250);

            service.buildLimitStats(categoryLimit, spent, DATE);

            verify(limitMapper).mapLimitStatsToDto(eq(categoryLimit), eq(spent), any(BigDecimal.class), percentageCaptor.capture(), statusCaptor.capture(), eq(DATE));
            assertEquals(CalculatePercentage.calculatePercentage(spent, LIMIT_AMOUNT), percentageCaptor.getValue());
            assertEquals(LimitStatus.fromPercentage(percentageCaptor.getValue()), statusCaptor.getValue());
        }

        @Test
        void shouldThrowExceptionWhenSpentIsNull() {
            assertThrows(NullPointerException.class, () -> service.buildLimitStats(categoryLimit, null, DATE));
            verifyNoInteractions(limitMapper);
        }
    }
}