package com.finovara.finovarabackend.accountactivity.revenue.service.get;

import com.finovara.finovarabackend.accountactivity.model.SortType;
import com.finovara.finovarabackend.accountactivity.revenue.dto.RevenueActivityDto;
import com.finovara.finovarabackend.accountactivity.revenue.mapper.RevenueActivityMapper;
import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivity;
import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivityType;
import com.finovara.finovarabackend.accountactivity.revenue.repository.RevenueActivityRepository;
import com.finovara.finovarabackend.accountactivity.revenue.service.RevenueActivityService;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetRevenueActivityTest {

    @Mock
    private RevenueActivityRepository revenueActivityRepository;
    @Mock
    private RevenueActivityMapper revenueActivityMapper;

    @InjectMocks
    private RevenueActivityService revenueActivityService;

    private final String EMAIL = "test@mail.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(revenueActivityService, "pageSize", 10);
    }

    @Test
    void shouldReturnActivitiesSortedByNewest() {

        RevenueActivity activity = new RevenueActivity();
        RevenueActivityDto dto = new RevenueActivityDto(
                RevenueActivityType.ADDED_REVENUE,
                new BigDecimal("100"),
                null,
                RevenueCategory.SALARY,
                null,
                LocalDateTime.now()
        );

        when(revenueActivityRepository.findByUserAssignedEmail(eq(EMAIL), any(Pageable.class))).thenReturn(List.of(activity));

        when(revenueActivityMapper.mapToRevenueActivity(activity)).thenReturn(dto);

        List<RevenueActivityDto> result = revenueActivityService.getRevenueActivity(EMAIL, SortType.NEWEST);

        assertEquals(1, result.size());
        assertEquals(dto, result.getFirst());

        verify(revenueActivityRepository).findByUserAssignedEmail(eq(EMAIL), any(Pageable.class));
        verify(revenueActivityMapper).mapToRevenueActivity(activity);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoActivities() {

        when(revenueActivityRepository.findByUserAssignedEmail(eq(EMAIL), any(Pageable.class))).thenReturn(List.of());

        List<RevenueActivityDto> result = revenueActivityService.getRevenueActivity(EMAIL, SortType.OLDEST);

        assertEquals(0, result.size());

        verify(revenueActivityRepository).findByUserAssignedEmail(eq(EMAIL), any(Pageable.class));
        verifyNoInteractions(revenueActivityMapper);
    }
}