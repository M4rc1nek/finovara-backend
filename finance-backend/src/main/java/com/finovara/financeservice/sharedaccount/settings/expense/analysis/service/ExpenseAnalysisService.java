package com.finovara.financeservice.sharedaccount.settings.expense.analysis.service;

import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.contracts.event.activity.settings.SettingsActivityEvent;
import com.finovara.contracts.model.activity.SettingActivityStatus;
import com.finovara.contracts.model.activity.SettingType;
import com.finovara.financeservice.exception.conflict.SmartScanConfirmationRequiredException;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.settings.finances.expense.model.ExpenseSettings;
import com.finovara.financeservice.settings.finances.expense.repository.ExpenseSettingsRepository;
import com.finovara.financeservice.sharedaccount.settings.expense.analysis.dto.SmartScanDto;
import com.finovara.financeservice.sharedaccount.settings.expense.analysis.dto.SmartScanMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final ExpenseSettingsRepository expenseSettingsRepository;
    private final ExpenseRepository expenseRepository;
    private final AuthBackendClient authBackendClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public void saveSmartScan(Long userId, SmartScanDto settings) {
        ExpenseSettings expenseSettings = expenseSettingsRepository.findByUserId(userId);

        expenseSettings.setSmartScanEnabled(settings.smartScanEnabled());
        if (expenseSettings.isSmartScanEnabled()) {
            kafkaTemplate.send("activity.settings", new SettingsActivityEvent(userId, SettingType.EXPENSE_SMART_SCAN, SettingActivityStatus.ENABLED, LocalDateTime.now()));
        } else {
            kafkaTemplate.send("activity.settings", new SettingsActivityEvent(userId, SettingType.EXPENSE_SMART_SCAN, SettingActivityStatus.DISABLED, LocalDateTime.now()));
        }
    }

    @Transactional
    public SmartScanDto getSmartScan(Long userId) {
        ExpenseSettings expenseSettings = expenseSettingsRepository.findByUserId(userId);

        return new SmartScanDto(expenseSettings.isSmartScanEnabled());
    }

    @Transactional
    public void handleSmartScan(Long userId, ConfirmPasswordDto confirmPasswordDto, BigDecimal newExpenseAmount, SmartScanMode mode) {
        ExpenseSettings expenseSettings = expenseSettingsRepository.findByUserId(userId);

        if (!expenseSettings.isSmartScanEnabled()) return;

        boolean shouldScan = calculateQuantityExpense(userId, mode);

        if (!shouldScan) return;

        BigDecimal expenseAnomalyThreshold = calculateAnomalyThreshold(userId);

        if (newExpenseAmount.compareTo(expenseAnomalyThreshold) > 0) {
            requirePasswordConfirmation(userId, confirmPasswordDto);
        }
    }

    private boolean calculateQuantityExpense(Long userId, SmartScanMode mode) {
        long expenseCount = expenseRepository.countExpensesByUserId(userId);

        return switch (mode) {
            case ADD -> (expenseCount + 1) % SCAN_INTERVAL == 0;
            case EDIT -> expenseCount % 5 == 0;
        };
    }

    private BigDecimal calculateAnomalyThreshold(Long userId) {
        List<Expense> expenses = expenseRepository.findFiveLastByUserId(userId, PageRequest.of(0, 4));

        BigDecimal averageAmountExpense = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP);

        return averageAmountExpense.multiply(ANOMALY_MULTIPLIER);
    }

    private void requirePasswordConfirmation(Long userId, ConfirmPasswordDto confirmPasswordDto) {
        if (confirmPasswordDto == null || confirmPasswordDto.password() == null) {
            throw new SmartScanConfirmationRequiredException("Unusual expense detected. Password confirmation required.");
        }

            authBackendClient.verifyPassword(userId, confirmPasswordDto);
    }

}
