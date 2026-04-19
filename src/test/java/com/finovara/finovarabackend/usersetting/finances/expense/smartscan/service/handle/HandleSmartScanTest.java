package com.finovara.finovarabackend.usersetting.finances.expense.smartscan.service.handle;

import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.exception.conflict.SmartScanConfirmationRequiredException;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.service.SmartScanService;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordValidator;
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
    private PasswordValidator passwordValidator;

    @InjectMocks
    private SmartScanService smartScanService;

    private ExpenseSettings expenseSettings;
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setup() {
        User user = new User();
        user.setId(USER_ID);
        expenseSettings = new ExpenseSettings();
        user.setExpenseSettings(expenseSettings);

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
    }

    @Test
    void shouldDoNothingWhenSmartScanDisabled() {
        expenseSettings.setSmartScanEnabled(false);

        smartScanService.handleSmartScan(USER_ID, null, BigDecimal.valueOf(100), SmartScanMode.ADD);

        verifyNoInteractions(passwordValidator, expenseRepository);
    }

    @Test
    void shouldDoNothingWhenNotFifthExpense() {
        expenseSettings.setSmartScanEnabled(true);
        when(expenseRepository.countExpensesByUserAssignedId(USER_ID)).thenReturn(3L);

        smartScanService.handleSmartScan(USER_ID, null, BigDecimal.valueOf(100), SmartScanMode.ADD);

        verifyNoInteractions(passwordValidator);
    }

    @Test
    void shouldThrowExceptionWhenUnusualExpenseWithoutPassword() {
        expenseSettings.setSmartScanEnabled(true);
        when(expenseRepository.countExpensesByUserAssignedId(USER_ID)).thenReturn(4L);

        List<Expense> lastFive = IntStream.range(0, 4)
                .mapToObj(i -> {
                    Expense e = new Expense();
                    e.setAmount(BigDecimal.valueOf(100));
                    return e;
                })
                .toList();

        when(expenseRepository.findFiveLastByUserAssignedId(USER_ID, PageRequest.of(0, 4))).thenReturn(lastFive);

        BigDecimal newExpense = BigDecimal.valueOf(400);

        assertThrows(SmartScanConfirmationRequiredException.class, () -> smartScanService.handleSmartScan(USER_ID, null, newExpense, SmartScanMode.ADD));

        verifyNoInteractions(passwordValidator);
    }

    @Test
    void shouldConfirmPasswordWhenUnusualExpenseWithPassword() {
        expenseSettings.setSmartScanEnabled(true);
        when(expenseRepository.countExpensesByUserAssignedId(USER_ID)).thenReturn(4L);

        List<Expense> lastFive = IntStream.range(0, 4)
                .mapToObj(i -> {
                    Expense e = new Expense();
                    e.setAmount(BigDecimal.valueOf(100));
                    return e;
                })
                .toList();

        when(expenseRepository.findFiveLastByUserAssignedId(USER_ID, PageRequest.of(0, 4))).thenReturn(lastFive);

        BigDecimal newExpense = BigDecimal.valueOf(400);
        ConfirmPasswordDto passwordDto = new ConfirmPasswordDto("password");

        smartScanService.handleSmartScan(USER_ID, passwordDto, newExpense, SmartScanMode.ADD);

        verify(passwordValidator).validatePassword(USER_ID, passwordDto);
    }
}