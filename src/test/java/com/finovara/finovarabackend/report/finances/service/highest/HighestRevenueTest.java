package com.finovara.finovarabackend.report.finances.service.highest;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.report.finances.highestrevenue.dto.HighestRevenueDto;
import com.finovara.finovarabackend.report.finances.highestrevenue.service.HighestRevenueService;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.util.model.PeriodType;
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
class HighestRevenueTest {

    @Mock
    private RevenueRepository revenueRepository;

    @InjectMocks
    private HighestRevenueService highestRevenueService;

    private final Long USER_ID = 1L;

    private LocalDate today;
    private LocalDate monday;
    private LocalDate firstDayOfMonth;
    private List<HighestRevenueDto> mockResult;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        monday = today.with(DayOfWeek.MONDAY);
        firstDayOfMonth = today.withDayOfMonth(1);

        ReflectionTestUtils.setField(highestRevenueService, "pageSize", 5);

        mockResult = List.of(mock(HighestRevenueDto.class));
    }

    @Test
    void shouldReturnDailyHighestRevenues() {
        when(revenueRepository.findHighestRevenuesByUserAssignedIdAndPeriod(eq(USER_ID), eq(today), eq(today),
                any(Pageable.class))).thenReturn(mockResult);

        List<HighestRevenueDto> result = highestRevenueService.getHighestRevenue(USER_ID, PeriodType.DAILY);

        assertEquals(mockResult, result);

        verify(revenueRepository).findHighestRevenuesByUserAssignedIdAndPeriod(eq(USER_ID), eq(today), eq(today), any(Pageable.class)
        );
    }

    @Test
    void shouldReturnWeeklyHighestRevenues() {
        when(revenueRepository.findHighestRevenuesByUserAssignedIdAndPeriod(eq(USER_ID), eq(monday), eq(today),
                any(Pageable.class))).thenReturn(mockResult);

        List<HighestRevenueDto> result = highestRevenueService.getHighestRevenue(USER_ID, PeriodType.WEEKLY);

        assertEquals(mockResult, result);

        verify(revenueRepository).findHighestRevenuesByUserAssignedIdAndPeriod(eq(USER_ID), eq(monday), eq(today),
                any(Pageable.class));
    }

    @Test
    void shouldReturnMonthlyHighestRevenues() {
        when(revenueRepository.findHighestRevenuesByUserAssignedIdAndPeriod(eq(USER_ID), eq(firstDayOfMonth), eq(today),
                any(Pageable.class))).thenReturn(mockResult);

        List<HighestRevenueDto> result = highestRevenueService.getHighestRevenue(USER_ID, PeriodType.MONTHLY);

        assertEquals(mockResult, result);

        verify(revenueRepository).findHighestRevenuesByUserAssignedIdAndPeriod(eq(USER_ID), eq(firstDayOfMonth), eq(today),
                any(Pageable.class));
    }

    @Test
    void shouldThrowExceptionForNullPeriodType() {
        assertThrows(InvalidInputException.class, () -> highestRevenueService.getHighestRevenue(USER_ID, null));
    }
}