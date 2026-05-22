package com.finovara.finovarabackend.report.finances.highesttransactions.highestrevenue.service;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.report.finances.highesttransactions.highestrevenue.dto.HighestRevenueDto;
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

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HighestRevenueServiceTest {

    @Mock
    private RevenueRepository revenueRepository;

    @InjectMocks
    private HighestRevenueService highestRevenueService;

    private List<HighestRevenueDto> mockResult;

    private final LocalDate baseDate = LocalDate.now();
    private final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(highestRevenueService, "pageSize", 5);

        mockResult = List.of(mock(HighestRevenueDto.class));
    }

    @ParameterizedTest
    @EnumSource(PeriodType.class)
    void shouldReturnHighestRevenueInPeriod(PeriodType periodType) {
        LocalDate to = baseDate;
        LocalDate from = periodType.getStartDate(baseDate);

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