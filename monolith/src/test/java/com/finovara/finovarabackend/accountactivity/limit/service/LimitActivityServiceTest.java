package com.finovara.finovarabackend.accountactivity.limit.service;

import com.finovara.finovarabackend.accountactivity.limit.dto.LimitActivityDto;
import com.finovara.finovarabackend.accountactivity.limit.mapper.LimitActivityMapper;
import com.finovara.finovarabackend.accountactivity.limit.model.LimitActivity;
import com.finovara.finovarabackend.accountactivity.limit.model.LimitActivityType;
import com.finovara.finovarabackend.accountactivity.limit.repository.LimitActivityRepository;
import com.finovara.finovarabackend.limit.model.Limit;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.model.SortType;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LimitActivityServiceTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private LimitActivityRepository limitActivityRepository;
    @Mock
    private LimitActivityMapper limitActivityMapper;

    @InjectMocks
    private LimitActivityService limitActivityService;

    private static final Long USER_ID = 1L;

    private User user;
    private Limit limit;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);

        limit = new Limit();
        limit.setAmount(new BigDecimal("1000"));
        limit.setPeriodType(PeriodType.MONTHLY);

        ReflectionTestUtils.setField(limitActivityService, "pageSize", 10);
    }

    @Nested
    class CreateLimitActivity {
        @Test
        void shouldCreateLimitActivitySuccessfully() {
            LocalDateTime now = LocalDateTime.now();

            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            limitActivityService.createLimitActivity(USER_ID, LimitActivityType.ADDED_LIMIT, limit);

            ArgumentCaptor<LimitActivity> captor = ArgumentCaptor.forClass(LimitActivity.class);

            verify(limitActivityRepository).save(captor.capture());

            LimitActivity activity = captor.getValue();

            assertEquals(user, activity.getUserAssigned());
            assertEquals(LimitActivityType.ADDED_LIMIT, activity.getLimitActivityType());
            assertEquals(limit.getPeriodType(), activity.getPeriodType());
            assertEquals(0, activity.getAmount().compareTo(limit.getAmount()));
            assertTrue(!activity.getCreatedAt().isBefore(now));
        }

        @Test
        void shouldThrowExceptionWhenUserDoesNotExist() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenThrow(new UserNotFoundException("User not found"));

            assertThrows(UserNotFoundException.class, () -> limitActivityService.createLimitActivity(USER_ID, LimitActivityType.ADDED_LIMIT, limit));

            verify(limitActivityRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateLimitActivity {

        @Test
        void shouldUpdateLimitActivitySuccessfully() {
            LocalDateTime now = LocalDateTime.now();

            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            limitActivityService.updateLimitActivity(USER_ID, LimitActivityType.EDITED_LIMIT, limit, new BigDecimal("800"));

            ArgumentCaptor<LimitActivity> captor = ArgumentCaptor.forClass(LimitActivity.class);

            verify(limitActivityRepository).save(captor.capture());

            LimitActivity activity = captor.getValue();

            assertEquals(user, activity.getUserAssigned());
            assertEquals(LimitActivityType.EDITED_LIMIT, activity.getLimitActivityType());
            assertEquals(limit.getPeriodType(), activity.getPeriodType());
            assertEquals(0, activity.getAmount().compareTo(limit.getAmount()));
            assertEquals(0, activity.getPreviousAmount().compareTo(new BigDecimal("800")));
            assertTrue(!activity.getCreatedAt().isBefore(now));
        }

        @Test
        void shouldThrowExceptionWhenUserDoesNotExist() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenThrow(new UserNotFoundException("User not found"));

            assertThrows(UserNotFoundException.class, () -> limitActivityService.updateLimitActivity(USER_ID,
                    LimitActivityType.EDITED_LIMIT, limit, new BigDecimal("800")));

            verify(limitActivityRepository, never()).save(any());
        }
    }

    @Nested
    class GetLimitActivity {
        @Test
        void shouldReturnActivitiesSortedByNewest() {
            LimitActivity activity = new LimitActivity();

            LimitActivityDto dto = new LimitActivityDto(null, null, new BigDecimal("500"),
                    null, LocalDateTime.now());

            when(limitActivityRepository.findByUserAssignedId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of(activity));

            when(limitActivityMapper.mapToLimitActivity(activity)).thenReturn(dto);

            List<LimitActivityDto> result = limitActivityService.getLimitActivity(USER_ID, SortType.NEWEST);

            assertEquals(1, result.size());
            assertEquals(dto, result.getFirst());

            verify(limitActivityRepository).findByUserAssignedId(eq(USER_ID), any(Pageable.class));
            verify(limitActivityMapper).mapToLimitActivity(activity);
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoActivities() {
            when(limitActivityRepository.findByUserAssignedId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of());

            List<LimitActivityDto> result = limitActivityService.getLimitActivity(USER_ID, SortType.OLDEST);

            assertTrue(result.isEmpty());

            verify(limitActivityRepository).findByUserAssignedId(eq(USER_ID), any(Pageable.class));
            verifyNoInteractions(limitActivityMapper);
        }
    }
}