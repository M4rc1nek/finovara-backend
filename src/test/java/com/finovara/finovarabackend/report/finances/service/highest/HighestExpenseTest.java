package com.finovara.finovarabackend.report.finances.service.highest;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.finances.highestexpense.dto.HighestExpenseDto;
import com.finovara.finovarabackend.report.finances.highestexpense.service.HighestExpenseService;
import com.finovara.finovarabackend.util.model.PeriodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
class HighestExpenseTest {

    private static final Long USER_ID = 1L;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private HighestExpenseService highestExpenseService;

    private List<HighestExpenseDto> mockResult;

    private final LocalDate baseDate = LocalDate.now();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(highestExpenseService, "pageSize", 5);

        mockResult = List.of(mock(HighestExpenseDto.class));
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnHighestExpensesInPeriod(PeriodType periodType) {
        LocalDate from;
        LocalDate to = baseDate;

        switch (periodType) {
            case DAILY -> from = baseDate;
            case WEEKLY -> from = baseDate.with(DayOfWeek.MONDAY);
            case MONTHLY -> from = baseDate.withDayOfMonth(1);
            default -> throw new IllegalArgumentException("Unsupported period");
        }

        when(expenseRepository.findHighestExpensesByUserAssignedIdAndPeriod(eq(USER_ID), eq(from), eq(to), any(Pageable.class)))
                .thenReturn(mockResult);

        List<HighestExpenseDto> result = highestExpenseService.getHighestExpense(USER_ID, periodType);

        assertEquals(mockResult, result);

        verify(expenseRepository).findHighestExpensesByUserAssignedIdAndPeriod(eq(USER_ID), eq(from), eq(to), any(Pageable.class));
    }

    @Test
    void shouldThrowExceptionWhenUnsupportedPeriod (){
        assertThrows(InvalidInputException.class, () -> highestExpenseService.getHighestExpense(USER_ID, null));
    }

}