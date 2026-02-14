package com.finovara.finovarabackend.usersettings.finances.expense.smartscan.service;

import com.finovara.finovarabackend.usersettings.finances.expense.smartscan.exception.conflict.SmartScanConfirmationRequiredException;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersettings.finances.expense.model.ExpenseSettings;
import com.finovara.finovarabackend.usersettings.finances.expense.smartscan.dto.SmartScanDto;
import com.finovara.finovarabackend.usersettings.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import com.finovara.finovarabackend.util.confirmationpassword.service.PasswordConfirmationService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartScanService {

    private final UserManagerService userManagerService;
    private final ExpenseRepository expenseRepository;
    private final PasswordConfirmationService passwordConfirmationService;

    @Transactional
    public void saveSmartScan(String email, SmartScanDto settings) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        expenseSettings.setSmartScanEnabled(settings.smartScanEnabled());
        log.info("Saved SmartScan settings. IsEnabled: {}", settings.smartScanEnabled());
    }

    @Transactional
    public SmartScanDto getSmartScan(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        return new SmartScanDto(expenseSettings.isSmartScanEnabled());
    }

    @Transactional
    public void handleSmartScan(String email, ConfirmPasswordDto confirmPasswordDto, BigDecimal newExpenseAmount, SmartScanMode mode) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        if (!expenseSettings.isSmartScanEnabled()) return;

        long expenseCount = expenseRepository.countExpensesByUserAssignedId(user.getId());

        boolean shouldScan = switch (mode) {
            case ADD -> (expenseCount + 1) % 5 == 0;
            case EDIT -> expenseCount % 5 == 0;
        };

        if (!shouldScan) return;

        List<Expense> expenses = expenseRepository.findFiveLastByUserAssignedId(user.getId(), PageRequest.of(0, 4));

        BigDecimal averageAmountExpense = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP);

        BigDecimal expenseAnomalyThreshold = averageAmountExpense.multiply(BigDecimal.valueOf(3));

        if (newExpenseAmount.compareTo(expenseAnomalyThreshold) > 0) {

            if (confirmPasswordDto == null || confirmPasswordDto.password() == null) {
                throw new SmartScanConfirmationRequiredException("Unusual expense detected. Password confirmation required.");
            }

            passwordConfirmationService.confirmPassword(user.getEmail(), confirmPasswordDto);

            log.info("Expense added successfully. Expense amount: {}", newExpenseAmount);
        }

    }
}
