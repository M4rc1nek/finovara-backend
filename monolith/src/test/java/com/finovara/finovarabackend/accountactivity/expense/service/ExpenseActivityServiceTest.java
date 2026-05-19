package com.finovara.finovarabackend.accountactivity.expense.service;

import com.finovara.finovarabackend.accountactivity.expense.dto.ExpenseActivityDto;
import com.finovara.finovarabackend.accountactivity.expense.mapper.ExpenseActivityMapper;
import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivity;
import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivityType;
import com.finovara.finovarabackend.accountactivity.expense.repository.ExpenseActivityRepository;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
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
class ExpenseActivityServiceTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private ExpenseActivityRepository expenseActivityRepository;
    @Mock
    private ExpenseActivityMapper expenseActivityMapper;

    @InjectMocks
    private ExpenseActivityService expenseActivityService;

    private static final Long USER_ID = 1L;

    private User user;
    private Expense expense;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);

        expense = new Expense();
        expense.setAmount(new BigDecimal("200"));
        expense.setCategory(ExpenseCategory.FOOD);

        ReflectionTestUtils.setField(expenseActivityService, "pageSize", 1);
    }

    @Nested
    class CreateExpenseActivity {
        @Test
        void shouldCreateExpenseActivitySuccessfully() {
            LocalDateTime now = LocalDateTime.now();

            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            expenseActivityService.createExpenseActivity(USER_ID, ExpenseActivityType.EDITED_EXPENSE, expense);

            ArgumentCaptor<ExpenseActivity> captor = ArgumentCaptor.forClass(ExpenseActivity.class);

            verify(expenseActivityRepository).save(captor.capture());

            ExpenseActivity activity = captor.getValue();

            assertEquals(user, activity.getUserAssigned());
            assertEquals(ExpenseActivityType.EDITED_EXPENSE, activity.getType());
            assertEquals(expense.getAmount(), activity.getAmount());
            assertEquals(expense.getCategory(), activity.getCategory());
            assertFalse(activity.getCreatedAt().isBefore(now));
        }
    }

    @Nested
    class UpdateExpenseActivity {
        @Test
        void shouldUpdateExpenseActivitySuccessfully() {
            LocalDateTime now = LocalDateTime.now();

            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

            expenseActivityService.updateExpenseActivity(USER_ID, ExpenseActivityType.ADDED_EXPENSE, expense, new BigDecimal("150"),
                    ExpenseCategory.TRANSPORT);

            ArgumentCaptor<ExpenseActivity> captor = ArgumentCaptor.forClass(ExpenseActivity.class);

            verify(expenseActivityRepository).save(captor.capture());

            ExpenseActivity activity = captor.getValue();

            assertEquals(user, activity.getUserAssigned());
            assertEquals(expense.getAmount(), activity.getAmount());
            assertEquals(expense.getCategory(), activity.getCategory());
            assertEquals(new BigDecimal("150"), activity.getPreviousAmount());
            assertEquals(ExpenseCategory.TRANSPORT, activity.getPreviousCategory());
            assertFalse(activity.getCreatedAt().isBefore(now));
        }

        @Test
        void shouldThrowExceptionWhenUserDoesNotExist() {
            when(userManagerService.getUserByIdOrThrow(USER_ID)).thenThrow(new UserNotFoundException("User not found"));

            assertThrows(UserNotFoundException.class, () -> expenseActivityService.updateExpenseActivity(USER_ID,
                    ExpenseActivityType.EDITED_EXPENSE, expense, new BigDecimal("150"), ExpenseCategory.FOOD));

            verify(expenseActivityRepository, never()).save(any());
        }
    }

    @Nested
    class GetExpenseActivity {
        @Test
        void shouldReturnActivitiesSortedByNewest() {
            ExpenseActivity activity = new ExpenseActivity();

            ExpenseActivityDto dto = new ExpenseActivityDto(ExpenseActivityType.ADDED_EXPENSE, new BigDecimal(100),
                    null, ExpenseCategory.FOOD, null, LocalDateTime.now());

            when(expenseActivityRepository.findByUserAssignedId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of(activity));

            when(expenseActivityMapper.mapToExpenseActivity(activity)).thenReturn(dto);

            List<ExpenseActivityDto> result = expenseActivityService.getExpenseActivity(USER_ID, SortType.NEWEST);

            assertEquals(1, result.size());
            assertEquals(dto, result.getFirst());

            verify(expenseActivityRepository).findByUserAssignedId(eq(USER_ID), any(Pageable.class));
            verify(expenseActivityMapper).mapToExpenseActivity(activity);
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoActivities() {
            when(expenseActivityRepository.findByUserAssignedId(eq(USER_ID), any(Pageable.class))).thenReturn(List.of());

            List<ExpenseActivityDto> result = expenseActivityService.getExpenseActivity(USER_ID, SortType.OLDEST);

            assertTrue(result.isEmpty());

            verify(expenseActivityRepository).findByUserAssignedId(eq(USER_ID), any(Pageable.class));
            verifyNoInteractions(expenseActivityMapper);
        }
    }
}