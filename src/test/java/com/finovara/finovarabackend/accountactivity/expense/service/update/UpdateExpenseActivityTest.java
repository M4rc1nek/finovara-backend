package com.finovara.finovarabackend.accountactivity.expense.service.update;

import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivityType;
import com.finovara.finovarabackend.accountactivity.expense.repository.ExpenseActivityRepository;
import com.finovara.finovarabackend.accountactivity.expense.service.ExpenseActivityService;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateExpenseActivityTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private ExpenseActivityRepository expenseActivityRepository;

    @InjectMocks
    private ExpenseActivityService expenseActivityService;

    private final String EMAIL = "test@mail.com";

    @Test
    void shouldUpdateExpenseActivitySuccessfully() {
        User user = new User();
        user.setId(1L);

        Expense expense = new Expense();
        expense.setAmount(new BigDecimal("200"));
        expense.setCategory(ExpenseCategory.FOOD);
        LocalDateTime now = LocalDateTime.now();

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);

        expenseActivityService.updateExpenseActivity(
                EMAIL,
                ExpenseActivityType.ADDED_EXPENSE,
                expense,
                new BigDecimal("150"),
                ExpenseCategory.TRANSPORT
        );

        verify(expenseActivityRepository).save(argThat(activity ->
                activity.getUserAssigned().equals(user) &&
                        activity.getAmount().equals(new BigDecimal("200")) &&
                        activity.getCategory() == ExpenseCategory.FOOD &&
                        activity.getPreviousAmount().equals(new BigDecimal("150")) &&
                        activity.getPreviousCategory() == ExpenseCategory.TRANSPORT &&
                        !activity.getDate().isBefore(now)
        ));
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        Expense expense = new Expense();
        expense.setAmount(new BigDecimal("200"));
        expense.setCategory(ExpenseCategory.FOOD);

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenThrow(new UserNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () ->
                expenseActivityService.updateExpenseActivity(
                        EMAIL,
                        ExpenseActivityType.EDITED_EXPENSE,
                        expense,
                        new BigDecimal("150"),
                        ExpenseCategory.FOOD
                )
        );

        verify(expenseActivityRepository, never()).save(any());
    }
}