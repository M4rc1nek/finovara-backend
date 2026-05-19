package com.finovara.finovarabackend.accountactivity.revenue.service;

import com.finovara.finovarabackend.accountactivity.revenue.dto.RevenueActivityDto;
import com.finovara.finovarabackend.accountactivity.revenue.mapper.RevenueActivityMapper;
import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivity;
import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivityType;
import com.finovara.finovarabackend.accountactivity.revenue.repository.RevenueActivityRepository;
import com.finovara.finovarabackend.revenue.model.Revenue;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
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
class RevenueActivityServiceTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private RevenueActivityRepository revenueActivityRepository;
    @Mock
    private RevenueActivityMapper revenueActivityMapper;

    @InjectMocks
    private RevenueActivityService revenueActivityService;

    private static final Long USER_ID = 1L;

    private User user;
    private Revenue revenue;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);

        revenue = new Revenue();
        revenue.setAmount(new BigDecimal("2000"));
        revenue.setCategory(RevenueCategory.BONUS);

        ReflectionTestUtils.setField(revenueActivityService, "pageSize", 10);
    }

    @Nested
    class CreateRevenueActivity {

        @Test
        void shouldCreateRevenueActivitySuccessfully() {
            LocalDateTime now = LocalDateTime.now();

            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            revenueActivityService.createRevenueActivity(USER_ID, RevenueActivityType.ADDED_REVENUE, revenue);

            ArgumentCaptor<RevenueActivity> captor = ArgumentCaptor.forClass(RevenueActivity.class);

            verify(revenueActivityRepository).save(captor.capture());

            RevenueActivity activity = captor.getValue();

            assertEquals(user, activity.getUserAssigned());
            assertEquals(RevenueActivityType.ADDED_REVENUE, activity.getType());
            assertEquals(0, activity.getAmount().compareTo(revenue.getAmount()));
            assertEquals(revenue.getCategory(), activity.getCategory());
            assertTrue(!activity.getCreatedAt().isBefore(now));
        }

        @Test
        void shouldThrowExceptionWhenUserDoesNotExist() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenThrow(new UserNotFoundException("User not found"));

            assertThrows(UserNotFoundException.class, () -> revenueActivityService.createRevenueActivity(USER_ID,
                    RevenueActivityType.ADDED_REVENUE, revenue));

            verify(revenueActivityRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateRevenueActivity {

        @Test
        void shouldUpdateRevenueActivitySuccessfully() {
            LocalDateTime now = LocalDateTime.now();

            BigDecimal previousAmount = new BigDecimal("1500");
            RevenueCategory previousCategory = RevenueCategory.SALARY;

            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            revenueActivityService.updateRevenueActivity(USER_ID, RevenueActivityType.EDITED_REVENUE, revenue, previousAmount, previousCategory);

            ArgumentCaptor<RevenueActivity> captor = ArgumentCaptor.forClass(RevenueActivity.class);

            verify(revenueActivityRepository).save(captor.capture());

            RevenueActivity activity = captor.getValue();

            assertEquals(user, activity.getUserAssigned());
            assertEquals(RevenueActivityType.EDITED_REVENUE, activity.getType());
            assertEquals(0, activity.getAmount().compareTo(revenue.getAmount()));
            assertEquals(revenue.getCategory(), activity.getCategory());
            assertEquals(0, activity.getPreviousAmount().compareTo(previousAmount));
            assertEquals(previousCategory, activity.getPreviousCategory());
            assertTrue(!activity.getCreatedAt().isBefore(now));
        }

        @Test
        void shouldThrowExceptionWhenUserDoesNotExist() {
            BigDecimal previousAmount = new BigDecimal("1500");
            RevenueCategory previousCategory = RevenueCategory.SALARY;

            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenThrow(new UserNotFoundException("User not found"));

            assertThrows(UserNotFoundException.class, () -> revenueActivityService.updateRevenueActivity(USER_ID,
                    RevenueActivityType.EDITED_REVENUE, revenue, previousAmount, previousCategory));

            verify(revenueActivityRepository, never()).save(any());
        }
    }

    @Nested
    class GetRevenueActivity {

        @Test
        void shouldReturnActivitiesSortedByNewest() {
            RevenueActivity activity = new RevenueActivity();

            RevenueActivityDto dto = new RevenueActivityDto(RevenueActivityType.ADDED_REVENUE, new BigDecimal("100"),
                    null, RevenueCategory.SALARY, null, LocalDateTime.now());

            when(revenueActivityRepository.findByUserAssignedId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of(activity));

            when(revenueActivityMapper.mapToRevenueActivity(activity)).thenReturn(dto);

            List<RevenueActivityDto> result = revenueActivityService.getRevenueActivity(USER_ID, SortType.NEWEST);

            assertEquals(1, result.size());
            assertEquals(dto, result.getFirst());

            verify(revenueActivityRepository).findByUserAssignedId(eq(USER_ID), any(Pageable.class));
            verify(revenueActivityMapper).mapToRevenueActivity(activity);
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoActivities() {
            when(revenueActivityRepository.findByUserAssignedId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of());

            List<RevenueActivityDto> result = revenueActivityService.getRevenueActivity(USER_ID, SortType.OLDEST);

            assertTrue(result.isEmpty());

            verify(revenueActivityRepository).findByUserAssignedId(eq(USER_ID), any(Pageable.class));
            verifyNoInteractions(revenueActivityMapper);
        }
    }
}