package com.finovara.finovarabackend.util.time;

import com.finovara.finovarabackend.config.TimeConfig;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.util.service.time.SpentInPeriodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpentInPeriodServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private TimeConfig timeConfig;

    @InjectMocks
    private SpentInPeriodService spentInPeriodService;

    private final Long USER_ID = 1L;
    private final LocalDate FIXED_DATE = LocalDate.of(2025, 3, 5);

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                FIXED_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );

        when(timeConfig.clock()).thenReturn(fixedClock);
    }

    @Test
    void shouldReturnTodayDate() {
        LocalDate today = spentInPeriodService.today();

        assertThat(today).isEqualTo(FIXED_DATE);
    }

    @Test
    void shouldReturnSpentToday() {
        when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, FIXED_DATE, FIXED_DATE))
                .thenReturn(BigDecimal.valueOf(50));

        BigDecimal result = spentInPeriodService.getSpentToday(USER_ID);

        assertThat(result).isEqualByComparingTo("50");
    }

    @Test
    void shouldReturnZeroWhenNoExpensesToday() {
        when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, FIXED_DATE, FIXED_DATE))
                .thenReturn(null);

        BigDecimal result = spentInPeriodService.getSpentToday(USER_ID);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldCalculateWeeklySpent() {
        LocalDate monday = FIXED_DATE.with(DayOfWeek.MONDAY);

        when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, monday, FIXED_DATE))
                .thenReturn(BigDecimal.valueOf(120));

        BigDecimal result = spentInPeriodService.getSpentWeekly(USER_ID);

        assertThat(result).isEqualByComparingTo("120");
    }

    @Test
    void shouldCalculateMonthlySpent() {
        LocalDate firstDayOfMonth = FIXED_DATE.withDayOfMonth(1);

        when(expenseRepository.sumExpensesByUserAndDateRange(USER_ID, firstDayOfMonth, FIXED_DATE))
                .thenReturn(BigDecimal.valueOf(300));

        BigDecimal result = spentInPeriodService.getSpentMonthly(USER_ID);

        assertThat(result).isEqualByComparingTo("300");
    }

    @Test
    void shouldCallRepositoryWithCorrectDatesForWeekly() {
        spentInPeriodService.getSpentWeekly(USER_ID);

        LocalDate monday = FIXED_DATE.with(DayOfWeek.MONDAY);

        verify(expenseRepository)
                .sumExpensesByUserAndDateRange(USER_ID, monday, FIXED_DATE);
    }
}