package com.finovara.activitylogservice.activitylog.mainaccount.expense.service;

import com.finovara.activitylogservice.activitylog.mainaccount.expense.dto.ExpenseActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.expense.dto.ExpenseActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.expense.mapper.ExpenseActivityMapper;
import com.finovara.activitylogservice.activitylog.mainaccount.expense.model.ExpenseActivity;
import com.finovara.activitylogservice.activitylog.mainaccount.expense.repository.ExpenseActivityRepository;
import com.finovara.contracts.mainaccount.activity.event.expense.ExpenseActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.ExpenseActivityType;
import com.finovara.contracts.util.SortType;
import com.finovara.contracts.util.model.ExpenseCategory;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseActivityServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 5, 25, 10, 0);

    @Mock
    private ExpenseActivityRepository expenseActivityRepository;

    @Mock
    private ExpenseActivityMapper expenseActivityMapper;

    @InjectMocks
    private ExpenseActivityService expenseActivityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(expenseActivityService, "pageSize", 10);
    }

    @Nested
    class FilterExpenseActivity {

        @Test
        void shouldFilterExpensesUsingCreatedDateRange() {
            ExpenseActivityFilterDto filter = new ExpenseActivityFilterDto(
                    ExpenseActivityType.ADDED_EXPENSE,
                    new BigDecimal("100.00"),
                    new BigDecimal("90.00"),
                    ExpenseCategory.FOOD,
                    ExpenseCategory.TRANSPORT,
                    LocalDate.of(2026, 5, 25)
            );
            ExpenseActivity activity = ExpenseActivity.builder().userId(USER_ID).build();
            ExpenseActivityDto dto = new ExpenseActivityDto(
                    ExpenseActivityType.ADDED_EXPENSE,
                    new BigDecimal("100.00"),
                    new BigDecimal("90.00"),
                    ExpenseCategory.FOOD,
                    ExpenseCategory.TRANSPORT,
                    OCCURRED_AT
            );

            when(expenseActivityRepository.findFilteredActivities(eq(USER_ID), eq(filter), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of(activity));
            when(expenseActivityMapper.mapToExpenseActivity(activity)).thenReturn(dto);

            List<ExpenseActivityDto> result = expenseActivityService.filterExpenseActivity(USER_ID, filter, 1);

            assertThat(result).containsExactly(dto);
            verify(expenseActivityRepository).findFilteredActivities(eq(USER_ID), eq(filter), any(), any(), any(Pageable.class));
        }
    }

    @Nested
    class DeleteByUserId {

        @Test
        void shouldDeleteUserActivitiesWhenUserIdIsValid() {
            expenseActivityService.deleteByUserId(USER_ID);

            verify(expenseActivityRepository).findAllByUserId(USER_ID);
            verify(expenseActivityRepository).deleteAll(anyList());
        }
    }

    @Nested
    class HandleEvent {

        @Test
        void shouldBuildEntityAndSaveViaRepository() {
            ExpenseActivityEvent event = new ExpenseActivityEvent(
                    USER_ID,
                    ExpenseActivityType.EDITED_EXPENSE,
                    new BigDecimal("200.00"),
                    ExpenseCategory.FOOD,
                    new BigDecimal("150.00"),
                    ExpenseCategory.TRANSPORT,
                    OCCURRED_AT
            );

            expenseActivityService.handleEvent(event);

            ArgumentCaptor<ExpenseActivity> captor = ArgumentCaptor.forClass(ExpenseActivity.class);
            verify(expenseActivityRepository).save(captor.capture());

            ExpenseActivity activity = captor.getValue();
            assertThat(activity.getUserId()).isEqualTo(USER_ID);
            assertThat(activity.getType()).isEqualTo(event.type());
            assertThat(activity.getAmount()).isEqualByComparingTo(event.amount());
            assertThat(activity.getCategory()).isEqualTo(event.category());
            assertThat(activity.getPreviousAmount()).isEqualByComparingTo(event.previousAmount());
            assertThat(activity.getPreviousCategory()).isEqualTo(event.previousCategory());
            assertThat(activity.getCreatedAt()).isEqualTo(OCCURRED_AT);
        }
    }

    @Nested
    class GetExpenseActivity {

        @Test
        void shouldReturnMappedActivities() {
            ExpenseActivity activity = ExpenseActivity.builder().userId(USER_ID).build();
            ExpenseActivityDto dto = new ExpenseActivityDto(
                    ExpenseActivityType.ADDED_EXPENSE,
                    new BigDecimal("100.00"),
                    null,
                    ExpenseCategory.FOOD,
                    null,
                    OCCURRED_AT
            );

            when(expenseActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of(activity));
            when(expenseActivityMapper.mapToExpenseActivity(activity)).thenReturn(dto);

            List<ExpenseActivityDto> result = expenseActivityService.getExpenseActivity(USER_ID, SortType.NEWEST, 1);

            assertThat(result).containsExactly(dto);
            verify(expenseActivityRepository).findByUserId(eq(USER_ID), any(Pageable.class));
            verify(expenseActivityMapper).mapToExpenseActivity(activity);
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoActivities() {
            when(expenseActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of());

            List<ExpenseActivityDto> result = expenseActivityService.getExpenseActivity(USER_ID, SortType.OLDEST, 1);

            assertThat(result).isEmpty();
            verifyNoInteractions(expenseActivityMapper);
        }
    }
}
