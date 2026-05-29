package com.finovara.activityservice.activity_log.accountactivity.limit.service;

import com.finovara.activityservice.activitylog.accountactivity.limit.dto.LimitActivityDto;
import com.finovara.activityservice.activitylog.accountactivity.limit.mapper.LimitActivityMapper;
import com.finovara.activityservice.activitylog.accountactivity.limit.model.LimitActivity;
import com.finovara.activityservice.activitylog.accountactivity.limit.repository.LimitActivityRepository;
import com.finovara.activityservice.activitylog.accountactivity.limit.service.LimitActivityService;
import com.finovara.contracts.event.limit.LimitActivityEvent;
import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.SortType;
import com.finovara.contracts.model.activity.LimitActivityType;
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
class LimitActivityServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 5, 25, 12, 0);

    @Mock
    private LimitActivityRepository limitActivityRepository;

    @Mock
    private LimitActivityMapper limitActivityMapper;

    @InjectMocks
    private LimitActivityService limitActivityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(limitActivityService, "pageSize", 10);
    }

    @Test
    void shouldSaveActivityFromEvent() {
        LimitActivityEvent event = new LimitActivityEvent(
                USER_ID,
                LimitActivityType.EDITED_LIMIT,
                PeriodType.MONTHLY.name(),
                new BigDecimal("1000.00"),
                new BigDecimal("800.00"),
                OCCURRED_AT
        );

        limitActivityService.handleEvent(event);

        ArgumentCaptor<LimitActivity> captor = ArgumentCaptor.forClass(LimitActivity.class);
        verify(limitActivityRepository).save(captor.capture());

        LimitActivity activity = captor.getValue();
        assertThat(activity.getUserId()).isEqualTo(USER_ID);
        assertThat(activity.getLimitActivityType()).isEqualTo(event.type());
        assertThat(activity.getPeriodType()).isEqualTo(PeriodType.MONTHLY);
        assertThat(activity.getAmount()).isEqualByComparingTo(event.amount());
        assertThat(activity.getPreviousAmount()).isEqualByComparingTo(event.previousAmount());
        assertThat(activity.getCreatedAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void shouldReturnMappedActivities() {
        LimitActivity activity = LimitActivity.builder().userId(USER_ID).build();
        LimitActivityDto dto = new LimitActivityDto(
                LimitActivityType.ADDED_LIMIT,
                PeriodType.WEEKLY,
                new BigDecimal("500.00"),
                null,
                OCCURRED_AT
        );

        when(limitActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of(activity));
        when(limitActivityMapper.mapToLimitActivity(activity)).thenReturn(dto);

        List<LimitActivityDto> result = limitActivityService.getLimitActivity(USER_ID, SortType.NEWEST);

        assertThat(result).containsExactly(dto);
        verify(limitActivityRepository).findByUserId(eq(USER_ID), any(Pageable.class));
        verify(limitActivityMapper).mapToLimitActivity(activity);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoActivities() {
        when(limitActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of());

        List<LimitActivityDto> result = limitActivityService.getLimitActivity(USER_ID, SortType.OLDEST);

        assertThat(result).isEmpty();
        verifyNoInteractions(limitActivityMapper);
    }
}
