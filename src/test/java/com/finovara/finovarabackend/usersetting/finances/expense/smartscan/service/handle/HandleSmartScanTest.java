package com.finovara.finovarabackend.usersetting.finances.expense.smartscan.service.handle;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.exception.conflict.SmartScanConfirmationRequiredException;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.service.SmartScanService;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordConfirmationService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandleSmartScanTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private PasswordConfirmationService passwordConfirmationService;

    @InjectMocks
    private SmartScanService smartScanService;

    private ExpenseSettings expenseSettings;
    private final String EMAIL = "test@test.com";

    @BeforeEach
    void setup() {
        User user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);
        expenseSettings = new ExpenseSettings();
        user.setExpenseSettings(expenseSettings);

        when(userManagerService.getUserByEmailOrThrow(EMAIL)).thenReturn(user);
    }

    @Test
    void shouldDoNothingWhenSmartScanDisabled() {
        expenseSettings.setSmartScanEnabled(false);

        smartScanService.handleSmartScan(EMAIL, null, BigDecimal.valueOf(100), SmartScanMode.ADD);

        verifyNoInteractions(passwordConfirmationService, expenseRepository);
    }

    @Test
    void shouldDoNothingWhenNotFifthExpense() {
        expenseSettings.setSmartScanEnabled(true);
        when(expenseRepository.countExpensesByUserAssignedId(1L)).thenReturn(3L);

        smartScanService.handleSmartScan(EMAIL, null, BigDecimal.valueOf(100), SmartScanMode.ADD);

        verifyNoInteractions(passwordConfirmationService);
    }

    @Test
    void shouldThrowExceptionWhenUnusualExpenseWithoutPassword() {
        expenseSettings.setSmartScanEnabled(true);
        when(expenseRepository.countExpensesByUserAssignedId(1L)).thenReturn(4L);

        List<Expense> lastFive = IntStream.range(0, 4)
                .mapToObj(i -> {
                    Expense e = new Expense();
                    e.setAmount(BigDecimal.valueOf(100));
                    return e;
                })
                .toList();

        when(expenseRepository.findFiveLastByUserAssignedId(1L, PageRequest.of(0, 4))).thenReturn(lastFive);

        BigDecimal newExpense = BigDecimal.valueOf(400);

        assertThrows(SmartScanConfirmationRequiredException.class, () -> smartScanService.handleSmartScan(EMAIL, null, newExpense, SmartScanMode.ADD));

        verifyNoInteractions(passwordConfirmationService);
    }

    @Test
    void shouldConfirmPasswordWhenUnusualExpenseWithPassword() {
        expenseSettings.setSmartScanEnabled(true);
        when(expenseRepository.countExpensesByUserAssignedId(1L)).thenReturn(4L);

        List<Expense> lastFive = IntStream.range(0, 4)
                .mapToObj(i -> {
                    Expense e = new Expense();
                    e.setAmount(BigDecimal.valueOf(100));
                    return e;
                })
                .toList();

        when(expenseRepository.findFiveLastByUserAssignedId(1L, PageRequest.of(0, 4))).thenReturn(lastFive);

        BigDecimal newExpense = BigDecimal.valueOf(400);
        ConfirmPasswordDto passwordDto = new ConfirmPasswordDto("password");

        smartScanService.handleSmartScan(EMAIL, passwordDto, newExpense, SmartScanMode.ADD);

        verify(passwordConfirmationService).confirmPassword(EMAIL, passwordDto);
    }
}