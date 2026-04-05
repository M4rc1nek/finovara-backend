package com.finovara.finovarabackend.accountactivity.expense.service.create;

import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivityType;
import com.finovara.finovarabackend.accountactivity.expense.repository.ExpenseActivityRepository;
import com.finovara.finovarabackend.accountactivity.expense.service.ExpenseActivityService;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateExpenseActivityTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private ExpenseActivityRepository expenseActivityRepository;

    @InjectMocks
    private ExpenseActivityService expenseActivityService;

    @Test
    void shouldCreateExpenseActivitySuccessfully() {
        String email = "user@example.com";
        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        Expense expense = new Expense();
        expense.setAmount(new BigDecimal("100.50"));
        expense.setCategory(ExpenseCategory.FOOD);
        LocalDateTime now = LocalDateTime.now();

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);

        expenseActivityService.createExpenseActivity(email, ExpenseActivityType.EDITED_EXPENSE, expense);

        verify(expenseActivityRepository).save(argThat(activity ->
                activity.getUserAssigned().equals(user) &&
                        activity.getType() == ExpenseActivityType.EDITED_EXPENSE &&
                        activity.getAmount().equals(new BigDecimal("100.50")) &&
                        activity.getCategory() == ExpenseCategory.FOOD &&
                        !activity.getDate().isBefore(now)

        ));
    }
}