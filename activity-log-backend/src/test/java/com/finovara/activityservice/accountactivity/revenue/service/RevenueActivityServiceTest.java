package com.finovara.activityservice.activity_log.accountactivity.revenue.service;

import com.finovara.activityservice.activity_log.accountactivity.revenue.dto.RevenueActivityDto;
import com.finovara.activityservice.activity_log.accountactivity.revenue.mapper.RevenueActivityMapper;
import com.finovara.activityservice.activity_log.accountactivity.revenue.model.RevenueActivity;
import com.finovara.activityservice.activity_log.accountactivity.revenue.repository.RevenueActivityRepository;
import com.finovara.activityservice.contracts.event.revenue.RevenueActivityEvent;
import com.finovara.activityservice.contracts.model.SortType;
import com.finovara.activityservice.contracts.model.activity.RevenueActivityType;
import com.finovara.activityservice.contracts.model.transaction.RevenueCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueActivityServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 5, 25, 11, 0);

    @Mock
    private RevenueActivityRepository revenueActivityRepository;

    @Mock
    private RevenueActivityMapper revenueActivityMapper;

    @InjectMocks
    private RevenueActivityService revenueActivityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(revenueActivityService, "pageSize", 10);
    }

    @Test
    void shouldSaveActivityFromEvent() {
        RevenueActivityEvent event = new RevenueActivityEvent(
                USER_ID,
                RevenueActivityType.EDITED_REVENUE,
                new BigDecimal("2000.00"),
                RevenueCategory.BONUS,
                new BigDecimal("1500.00"),
                RevenueCategory.SALARY,
                OCCURRED_AT
        );

        revenueActivityService.handleEvent(event);

        ArgumentCaptor<RevenueActivity> captor = ArgumentCaptor.forClass(RevenueActivity.class);
        verify(revenueActivityRepository).save(captor.capture());

        RevenueActivity activity = captor.getValue();
        assertThat(activity.getUserId()).isEqualTo(USER_ID);
        assertThat(activity.getType()).isEqualTo(event.type());
        assertThat(activity.getAmount()).isEqualByComparingTo(event.amount());
        assertThat(activity.getCategory()).isEqualTo(event.category());
        assertThat(activity.getPreviousAmount()).isEqualByComparingTo(event.previousAmount());
        assertThat(activity.getPreviousCategory()).isEqualTo(event.previousCategory());
        assertThat(activity.getCreatedAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void shouldReturnMappedActivities() {
        RevenueActivity activity = RevenueActivity.builder().userId(USER_ID).build();
        RevenueActivityDto dto = new RevenueActivityDto(
                RevenueActivityType.ADDED_REVENUE,
                new BigDecimal("100.00"),
                null,
                RevenueCategory.SALARY,
                null,
                OCCURRED_AT
        );

        when(revenueActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of(activity));
        when(revenueActivityMapper.mapToRevenueActivity(activity)).thenReturn(dto);

        List<RevenueActivityDto> result = revenueActivityService.getRevenueActivity(USER_ID, SortType.NEWEST);

        assertThat(result).containsExactly(dto);
        verify(revenueActivityRepository).findByUserId(eq(USER_ID), any(Pageable.class));
        verify(revenueActivityMapper).mapToRevenueActivity(activity);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoActivities() {
        when(revenueActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of());

        List<RevenueActivityDto> result = revenueActivityService.getRevenueActivity(USER_ID, SortType.OLDEST);

        assertThat(result).isEmpty();
        verifyNoInteractions(revenueActivityMapper);
    }
}
