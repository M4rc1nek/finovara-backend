package com.finovara.financeservice.sharedaccount.settings.expense.analysis.service;

import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettings;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettingsRepository;
import com.finovara.financeservice.sharedaccount.settings.expense.analysis.dto.ExpenseAnalysisDto;
import com.finovara.financeservice.sharedaccount.settings.expense.analysis.dto.ExpenseAnalysisMode;
import com.finovara.financeservice.util.settings.ExpenseAnomalyDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseAnalysisService {

    private static final int SCAN_INTERVAL = 10;
    private static final BigDecimal ANOMALY_MULTIPLIER = BigDecimal.valueOf(3);

    private final SharedAccountSettingsRepository sharedAccountSettingsRepository;
    private final SharedExpenseRepository sharedExpenseRepository;
    private final AuthBackendClient authBackendClient;
    private final ExpenseAnomalyDetector expenseAnomalyDetector;

    @Transactional
    public void saveExpenseAnalysis(Long userId, ExpenseAnalysisDto settings) {
        SharedAccountSettings sharedAccountSettings = sharedAccountSettingsRepository.findByUserId(userId);

        sharedAccountSettings.setExpenseAnalysisEnabled(settings.expenseAnalysisEnabled());
    }

    @Transactional
    public ExpenseAnalysisDto getExpenseAnalysis(Long userId) {
        SharedAccountSettings sharedAccountSettings = sharedAccountSettingsRepository.findByUserId(userId);

        return new ExpenseAnalysisDto(sharedAccountSettings.isExpenseAnalysisEnabled());
    }

    @Transactional
    public void handleExpenseAnalysis(Long userId, ConfirmPasswordDto confirmPasswordDto, BigDecimal newExpenseAmount, ExpenseAnalysisMode mode) {
        SharedAccountSettings sharedAccountSettings = sharedAccountSettingsRepository.findByUserId(userId);

        if (!sharedAccountSettings.isExpenseAnalysisEnabled()) return;

        boolean shouldScan = calculateQuantityExpense(userId, mode);

        if (!shouldScan) return;

        BigDecimal expenseAnomalyThreshold = calculateAnomalyThreshold(userId);

        if (newExpenseAmount.compareTo(expenseAnomalyThreshold) > 0) {
            expenseAnomalyDetector.requirePasswordConfirmation(userId, confirmPasswordDto, authBackendClient);
        }
    }

    private boolean calculateQuantityExpense(Long userId, ExpenseAnalysisMode mode) {
        long expenseCount = sharedExpenseRepository.countExpensesByUserId(userId);

        return switch (mode) {
            case ADD -> (expenseCount + 1) % SCAN_INTERVAL == 0;
            case EDIT -> expenseCount % SCAN_INTERVAL == 0;
        };
    }

    private BigDecimal calculateAnomalyThreshold(Long userId) {
        List<SharedExpense> sharedExpenses = sharedExpenseRepository.findTenLastByUserId(userId, PageRequest.of(0, 11));

        List<BigDecimal> amounts = sharedExpenses.stream().map(SharedExpense::getAmount).toList();

        return expenseAnomalyDetector.calculateAnomalyThreshold(amounts, ANOMALY_MULTIPLIER);
    }
}