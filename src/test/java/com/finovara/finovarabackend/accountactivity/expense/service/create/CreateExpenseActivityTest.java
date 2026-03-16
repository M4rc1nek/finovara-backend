package com.finovara.finovarabackend.accountactivity.expense.service;

import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivityType;
import com.finovara.finovarabackend.accountactivity.expense.repository.ExpenseActivityRepository;
import com.finovara.finovarabackend.config.TimeConfig;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateExpenseActivityTest {

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private TimeConfig timeConfig;
    @Mock
    private ExpenseActivityRepository expenseActivityRepository;

    @InjectMocks
    private ExpenseActivityService expenseActivityService;

    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), ZoneId.of("UTC"));
        when(timeConfig.clock()).thenReturn(fixedClock);
    }

    @Test
    void shouldCreateExpenseActivitySuccessfully() {
        String email = "user@example.com";
        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        Expense expense = new Expense();
        expense.setAmount(new BigDecimal("100.50"));
        expense.setCategory(ExpenseCategory.FOOD);

        when(userManagerService.getUserByEmailOrThrow(email)).thenReturn(user);

        expenseActivityService.createExpenseActivity(email, ExpenseActivityType.EDITED_EXPENSE, expense);

        verify(expenseActivityRepository).save(argThat(activity ->
                activity.getUserAssigned().equals(user) &&
                        activity.getType() == ExpenseActivityType.EDITED_EXPENSE &&
                        activity.getAmount().equals(new BigDecimal("100.50")) &&
                        activity.getCategory() == ExpenseCategory.FOOD &&
                        activity.getDate().equals(LocalDateTime.now(fixedClock))
        ));
    }
}