package com.finovara.activityservice.activity_log.accountactivity.expense.service;

import com.finovara.activityservice.activitylog.accountactivity.expense.dto.ExpenseActivityDto;
import com.finovara.activityservice.activitylog.accountactivity.expense.mapper.ExpenseActivityMapper;
import com.finovara.activityservice.activitylog.accountactivity.expense.model.ExpenseActivity;
import com.finovara.activityservice.activitylog.accountactivity.expense.repository.ExpenseActivityRepository;
import com.finovara.activityservice.activitylog.accountactivity.expense.service.ExpenseActivityService;
import com.finovara.contracts.event.expense.ExpenseActivityEvent;
import com.finovara.contracts.model.SortType;
import com.finovara.contracts.model.activity.ExpenseActivityType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
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

    @Test
    void shouldSaveActivityFromEvent() {
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

        List<ExpenseActivityDto> result = expenseActivityService.getExpenseActivity(USER_ID, SortType.NEWEST);

        assertThat(result).containsExactly(dto);
        verify(expenseActivityRepository).findByUserId(eq(USER_ID), any(Pageable.class));
        verify(expenseActivityMapper).mapToExpenseActivity(activity);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoActivities() {
        when(expenseActivityRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of());

        List<ExpenseActivityDto> result = expenseActivityService.getExpenseActivity(USER_ID, SortType.OLDEST);

        assertThat(result).isEmpty();
        verifyNoInteractions(expenseActivityMapper);
    }
}
