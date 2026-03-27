package com.finovara.finovarabackend.report.finances.service.highest;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.finances.highestexpense.dto.ReportsHighestExpense;
import com.finovara.finovarabackend.report.finances.highestexpense.service.HighestExpenseService;
import com.finovara.finovarabackend.report.model.ReportPeriodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportsHighestExpenseTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private HighestExpenseService highestExpenseService;

    private final Long USER_ID = 1L;

    private LocalDate today;
    private LocalDate monday;
    private LocalDate firstDayOfMonth;
    private List<ReportsHighestExpense> mockResult;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        monday = today.with(DayOfWeek.MONDAY);
        firstDayOfMonth = today.withDayOfMonth(1);
        ReflectionTestUtils.setField(highestExpenseService,"pageSize", 5 );
        mockResult = List.of(mock(ReportsHighestExpense.class));
    }

    @Test
    void shouldReturnDailyHighestExpenses() {
        when(expenseRepository.findHighestExpensesByUserAssignedIdAndPeriod(eq(USER_ID), eq(today), eq(today), any(Pageable.class))).thenReturn(mockResult);

        List<ReportsHighestExpense> result = highestExpenseService.getHighestExpense(USER_ID, ReportPeriodType.DAILY);

        assertEquals(mockResult, result);

        verify(expenseRepository).findHighestExpensesByUserAssignedIdAndPeriod(eq(USER_ID), eq(today), eq(today), any(Pageable.class));
    }

    @Test
    void shouldReturnWeeklyHighestExpenses() {
        when(expenseRepository.findHighestExpensesByUserAssignedIdAndPeriod(eq(USER_ID), eq(monday), eq(today), any(Pageable.class))).thenReturn(mockResult);

        List<ReportsHighestExpense> result = highestExpenseService.getHighestExpense(USER_ID, ReportPeriodType.WEEKLY);

        assertEquals(mockResult, result);

        verify(expenseRepository).findHighestExpensesByUserAssignedIdAndPeriod(eq(USER_ID), eq(monday), eq(today), any(Pageable.class));
    }

    @Test
    void shouldReturnMonthlyHighestExpenses() {
        when(expenseRepository.findHighestExpensesByUserAssignedIdAndPeriod(eq(USER_ID), eq(firstDayOfMonth), eq(today), any(Pageable.class))).thenReturn(mockResult);

        List<ReportsHighestExpense> result = highestExpenseService.getHighestExpense(USER_ID, ReportPeriodType.MONTHLY);

        assertEquals(mockResult, result);

        verify(expenseRepository).findHighestExpensesByUserAssignedIdAndPeriod(eq(USER_ID), eq(firstDayOfMonth), eq(today), any(Pageable.class));
    }

    @Test
    void shouldThrowExceptionForUnsupportedReportType() {
        assertThrows(InvalidInputException.class, () -> highestExpenseService.getHighestExpense(USER_ID, null));

    }
}
