package com.finovara.finovarabackend.accountactivity.expense.service.get;

import com.finovara.finovarabackend.accountactivity.model.SortType;
import com.finovara.finovarabackend.accountactivity.expense.dto.ExpenseActivityDto;
import com.finovara.finovarabackend.accountactivity.expense.mapper.ExpenseActivityMapper;
import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivity;
import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivityType;
import com.finovara.finovarabackend.accountactivity.expense.repository.ExpenseActivityRepository;
import com.finovara.finovarabackend.accountactivity.expense.service.ExpenseActivityService;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
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
class GetExpenseActivityTest {

    @Mock
    private ExpenseActivityRepository expenseActivityRepository;

    @Mock
    private ExpenseActivityMapper expenseActivityMapper;

    @InjectMocks
    private ExpenseActivityService expenseActivityService;

    private final String EMAIL = "test@mail.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(expenseActivityService, "pageSize", 1);
    }

    @Test
    void shouldReturnActivitiesSortedByNewest() {

        ExpenseActivity activity = new ExpenseActivity();
        ExpenseActivityDto dto = new ExpenseActivityDto(
                ExpenseActivityType.ADDED_EXPENSE,
                new BigDecimal(100),
                null,
                ExpenseCategory.FOOD,
                null,
                LocalDateTime.now()
        );

        when(expenseActivityRepository.findByUserAssignedEmail(eq(EMAIL), any(Pageable.class))).thenReturn(List.of(activity));

        when(expenseActivityMapper.mapToExpenseActivity(activity)).thenReturn(dto);

        List<ExpenseActivityDto> result = expenseActivityService.getExpenseActivity(EMAIL, SortType.NEWEST);

        assertEquals(1, result.size());
        assertEquals(dto, result.getFirst());

        verify(expenseActivityRepository).findByUserAssignedEmail(eq(EMAIL), any(Pageable.class));
        verify(expenseActivityMapper).mapToExpenseActivity(activity);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoActivities() {

        when(expenseActivityRepository.findByUserAssignedEmail(eq(EMAIL), any(Pageable.class))).thenReturn(List.of());

        List<ExpenseActivityDto> result = expenseActivityService.getExpenseActivity(EMAIL, SortType.OLDEST);

        assertEquals(0, result.size());

        verify(expenseActivityRepository).findByUserAssignedEmail(eq(EMAIL), any(Pageable.class));
        verifyNoInteractions(expenseActivityMapper);
    }

}