package com.finovara.financeservice.limit.service;

import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.limit.dto.LimitSpendingDto;
import com.finovara.financeservice.limit.model.Limit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LimitSpendingServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long LIMIT_ID = 10L;
    private static final Long OTHER_LIMIT_ID = 11L;
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private static final PeriodType PERIOD = PeriodType.values()[0];
    private static final PeriodType OTHER_PERIOD = PeriodType.values()[1];

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private LimitSpendingService service;

    private Limit userLimit;
    private Limit otherUserLimit;
    private Limit otherPeriodLimit;

    @BeforeEach
    void setUp() {
        userLimit = limit(LIMIT_ID, USER_ID, PERIOD);
        otherUserLimit = limit(OTHER_LIMIT_ID, OTHER_USER_ID, PERIOD);
        otherPeriodLimit = limit(12L, USER_ID, OTHER_PERIOD);
    }

    @Nested
    class CalculateSpent {

        @Test
        void shouldQueryOncePerPeriodAndReturnSpendingForEveryLimit() {
            List<Limit> limits = List.of(userLimit, otherUserLimit, otherPeriodLimit);
            when(expenseRepository.sumExpensesByLimitIds(
                    List.of(LIMIT_ID, OTHER_LIMIT_ID), PERIOD.getStartDate(TODAY), TODAY))
                    .thenReturn(List.of(
                            new LimitSpendingDto(LIMIT_ID, BigDecimal.TEN),
                            new LimitSpendingDto(OTHER_LIMIT_ID, BigDecimal.valueOf(20))));
            when(expenseRepository.sumExpensesByLimitIds(
                    List.of(12L), OTHER_PERIOD.getStartDate(TODAY), TODAY))
                    .thenReturn(List.of(new LimitSpendingDto(12L, BigDecimal.valueOf(30))));

            Map<Long, BigDecimal> result = service.calculateSpent(limits, TODAY);

            assertEquals(Map.of(
                    LIMIT_ID, BigDecimal.TEN,
                    OTHER_LIMIT_ID, BigDecimal.valueOf(20),
                    12L, BigDecimal.valueOf(30)), result);
            verify(expenseRepository).sumExpensesByLimitIds(
                    List.of(LIMIT_ID, OTHER_LIMIT_ID), PERIOD.getStartDate(TODAY), TODAY);
            verify(expenseRepository).sumExpensesByLimitIds(
                    List.of(12L), OTHER_PERIOD.getStartDate(TODAY), TODAY);
        }

        @Test
        void shouldReturnZeroForLimitWithoutExpenses() {
            when(expenseRepository.sumExpensesByLimitIds(
                    List.of(LIMIT_ID), PERIOD.getStartDate(TODAY), TODAY))
                    .thenReturn(List.of(new LimitSpendingDto(LIMIT_ID, null)));

            Map<Long, BigDecimal> result = service.calculateSpent(List.of(userLimit), TODAY);

            assertEquals(BigDecimal.ZERO, result.get(LIMIT_ID));
        }

        @Test
        void shouldNotQueryWhenNoLimitsAreProvided() {
            Map<Long, BigDecimal> result = service.calculateSpent(List.of(), TODAY);

            assertTrue(result.isEmpty());
            verifyNoInteractions(expenseRepository);
        }

        @Test
        void shouldPropagateRepositoryFailure() {
            when(expenseRepository.sumExpensesByLimitIds(
                    List.of(LIMIT_ID), PERIOD.getStartDate(TODAY), TODAY))
                    .thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> service.calculateSpent(List.of(userLimit), TODAY));
        }
    }

    private Limit limit(Long id, Long userId, PeriodType period) {
        return Limit.builder()
                .id(id)
                .userId(userId)
                .periodType(period)
                .category(ExpenseCategory.values()[0])
                .amount(BigDecimal.valueOf(100))
                .build();
    }
}
