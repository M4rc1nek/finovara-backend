package com.finovara.corebackend.report.finances.highesttransactions.highestexpense.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.expense.repository.ExpenseRepository;
import com.finovara.corebackend.report.finances.highesttransactions.highestexpense.dto.HighestExpenseDto;
import com.finovara.contracts.model.PeriodType;
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

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HighestExpenseServiceTest {
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
        LocalDate to = baseDate;
        LocalDate from = periodType.getStartDate(baseDate);

        when(expenseRepository.findHighestExpensesByUserAssignedIdAndPeriod(eq(USER_ID), eq(from), eq(to), any(Pageable.class)))
                .thenReturn(mockResult);

        List<HighestExpenseDto> result = highestExpenseService.getHighestExpense(USER_ID, periodType);

        assertEquals(mockResult, result);

        verify(expenseRepository).findHighestExpensesByUserAssignedIdAndPeriod(eq(USER_ID), eq(from), eq(to), any(Pageable.class));
    }

    @Test
    void shouldThrowExceptionWhenUnsupportedPeriod() {
        assertThrows(InvalidInputException.class, () -> highestExpenseService.getHighestExpense(USER_ID, null));
    }
}