package com.finovara.corebackend.usersetting.finances.expense.smartscan.service;

import com.finovara.contracts.event.activity.settings.SettingsActivityEvent;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.corebackend.expense.model.Expense;
import com.finovara.corebackend.expense.repository.ExpenseRepository;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.usersetting.finances.expense.model.ExpenseSettings;
import com.finovara.corebackend.usersetting.finances.expense.smartscan.dto.SmartScanDto;
import com.finovara.corebackend.usersetting.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.corebackend.usersetting.finances.expense.smartscan.exception.conflict.SmartScanConfirmationRequiredException;
import com.finovara.contracts.dto.ConfirmPasswordDto;
import com.finovara.corebackend.util.confirmationpassword.service.PasswordValidator;
import com.finovara.corebackend.util.user.service.UserManagerService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartScanService {

    private static final int SCAN_INTERVAL = 5;
    private static final BigDecimal ANOMALY_MULTIPLIER = BigDecimal.valueOf(3);

    private final UserManagerService userManagerService;
    private final ExpenseRepository expenseRepository;
    private final PasswordValidator passwordValidator;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public void saveSmartScan(Long userId, SmartScanDto settings) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        expenseSettings.setSmartScanEnabled(settings.smartScanEnabled());
        if (expenseSettings.isSmartScanEnabled()) {
            kafkaTemplate.send("activity.settings", new SettingsActivityEvent(userId, SettingType.EXPENSE_SMART_SCAN, SettingActivityStatus.ENABLED, LocalDateTime.now()));
        } else {
            kafkaTemplate.send("activity.settings", new SettingsActivityEvent(userId, SettingType.EXPENSE_SMART_SCAN, SettingActivityStatus.DISABLED, LocalDateTime.now()));
        }
    }

    @Transactional
    public SmartScanDto getSmartScan(Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        return new SmartScanDto(expenseSettings.isSmartScanEnabled());
    }

    @Transactional
    public void handleSmartScan(Long userId, ConfirmPasswordDto confirmPasswordDto, BigDecimal newExpenseAmount, SmartScanMode mode) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        ExpenseSettings expenseSettings = user.getExpenseSettings();

        if (!expenseSettings.isSmartScanEnabled()) return;

        boolean shouldScan = calculateQuantityExpense(user, mode);

        if (!shouldScan) return;

        BigDecimal expenseAnomalyThreshold = calculateAnomalyThreshold(user);

        if (newExpenseAmount.compareTo(expenseAnomalyThreshold) > 0) {
            requirePasswordConfirmation(userId, confirmPasswordDto);
        }
    }

    private boolean calculateQuantityExpense(User user, SmartScanMode mode) {
        long expenseCount = expenseRepository.countExpensesByUserAssignedId(user.getId());

        return switch (mode) {
            case ADD -> (expenseCount + 1) % SCAN_INTERVAL == 0;
            case EDIT -> expenseCount % 5 == 0;
        };
    }

    private BigDecimal calculateAnomalyThreshold(User user) {
        List<Expense> expenses = expenseRepository.findFiveLastByUserAssignedId(user.getId(), PageRequest.of(0, 4));

        BigDecimal averageAmountExpense = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP);

        return averageAmountExpense.multiply(ANOMALY_MULTIPLIER);
    }

    private void requirePasswordConfirmation(Long userId, ConfirmPasswordDto confirmPasswordDto) {
        if (confirmPasswordDto == null || confirmPasswordDto.password() == null) {
            throw new SmartScanConfirmationRequiredException("Unusual expense detected. Password confirmation required.");
        }

        passwordValidator.validatePassword(userId, confirmPasswordDto);
    }

}
