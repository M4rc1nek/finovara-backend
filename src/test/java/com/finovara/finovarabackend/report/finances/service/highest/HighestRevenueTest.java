package com.finovara.finovarabackend.report.finances.service.highest;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.report.finances.highestrevenue.dto.HighestRevenueDto;
import com.finovara.finovarabackend.report.finances.highestrevenue.service.HighestRevenueService;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
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
class HighestRevenueTest {

    @Mock
    private RevenueRepository revenueRepository;

    @InjectMocks
    private HighestRevenueService highestRevenueService;

    private List<HighestRevenueDto> mockResult;

    private final LocalDate baseDate = LocalDate.of(2026, 3, 29);
    private final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(highestRevenueService, "pageSize", 5);

        mockResult = List.of(mock(HighestRevenueDto.class));
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnHighestRevenueInPeriod(PeriodType periodType) {
        LocalDate from;
        LocalDate to = baseDate;

        switch (periodType) {
            case DAILY -> from = baseDate;
            case WEEKLY -> from = baseDate.with(DayOfWeek.MONDAY);
            case MONTHLY -> from = baseDate.withDayOfMonth(1);
            default -> throw new IllegalArgumentException("Unsupported period");
        }

        when(revenueRepository.findHighestRevenuesByUserAssignedIdAndPeriod(eq(USER_ID), eq(from), eq(to), any(Pageable.class)))
                .thenReturn(mockResult);

        List<HighestRevenueDto> result = highestRevenueService.getHighestRevenue(USER_ID, periodType);

        assertEquals(mockResult, result);

        verify(revenueRepository).findHighestRevenuesByUserAssignedIdAndPeriod(eq(USER_ID), eq(from), eq(to), any(Pageable.class));
    }

    @Test
    void shouldThrowExceptionForNullPeriodType() {
        assertThrows(InvalidInputException.class, () -> highestRevenueService.getHighestRevenue(USER_ID, null));
    }
}