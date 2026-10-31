package com.finovara.activitylogservice.activitylog.mainaccount.limit;

import com.finovara.activitylogservice.activitylog.mainaccount.limit.dto.LimitActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.limit.dto.LimitActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.limit.mapper.LimitActivityMapper;
import com.finovara.activitylogservice.activitylog.mainaccount.limit.model.LimitActivity;
import com.finovara.activitylogservice.activitylog.mainaccount.limit.repository.LimitActivityRepository;
import com.finovara.activitylogservice.activitylog.mainaccount.limit.service.LimitActivityService;
import com.finovara.contracts.mainaccount.activity.event.limit.LimitActivityEvent;
import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.SortType;
import com.finovara.contracts.mainaccount.activity.model.LimitActivityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Nested
    class FilterLimitActivity {

        @Test
        void shouldFilterLimitActivitiesUsingCreatedDateRange() {
            LimitActivityFilterDto filter = new LimitActivityFilterDto(
                    LimitActivityType.ADDED_LIMIT,
                    PeriodType.MONTHLY,
                    new BigDecimal("2500.00"),
                    new BigDecimal("2400.00"),
                    LocalDate.of(2026, 5, 25)
            );
            LimitActivity activity = LimitActivity.builder().userId(USER_ID).build();
            LimitActivityDto dto = new LimitActivityDto(
                    LimitActivityType.ADDED_LIMIT,
                    PeriodType.MONTHLY,
                    new BigDecimal("2500.00"),
                    new BigDecimal("2400.00"),
                    OCCURRED_AT
            );

            when(limitActivityRepository.findFilteredActivities(eq(USER_ID), eq(filter), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of(activity));
            when(limitActivityMapper.mapToLimitActivity(activity)).thenReturn(dto);

            List<LimitActivityDto> result = limitActivityService.filterLimitActivity(USER_ID, filter, 1);

            assertThat(result).containsExactly(dto);
            verify(limitActivityRepository).findFilteredActivities(eq(USER_ID), eq(filter), any(), any(), any(Pageable.class));
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteUserActivitiesWhenUserIdIsValid() {
            limitActivityService.deleteByUserId(USER_ID);

            verify(limitActivityRepository).findAllByUserId(USER_ID);
            verify(limitActivityRepository).deleteAll(any());
        }
    }

    @Nested
    class HandleEvent {

        @Test
        void shouldBuildEntityAndSaveViaRepository() {
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
    }

    @Nested
    class GetLimitActivity {

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

            List<LimitActivityDto> result = limitActivityService.getLimitActivity(USER_ID, SortType.NEWEST, 1);

            assertThat(result).containsExactly(dto);
            verify(limitActivityRepository).findByUserId(eq(USER_ID), any(Pageable.class));
            verify(limitActivityMapper).mapToLimitActivity(activity);
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoActivities() {
            when(limitActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of());

            List<LimitActivityDto> result = limitActivityService.getLimitActivity(USER_ID, SortType.OLDEST, 1);

            assertThat(result).isEmpty();
            verifyNoInteractions(limitActivityMapper);
        }
    }
}
