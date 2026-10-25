package com.finovara.financeservice.sharedaccount.expense.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseDto;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseRequest;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseResponse;
import com.finovara.financeservice.sharedaccount.expense.mapper.SharedExpenseMapper;
import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.limit.model.SharedLimit;
import com.finovara.financeservice.sharedaccount.limit.repository.SharedLimitRepository;
import com.finovara.financeservice.sharedaccount.limit.service.SharedLimitSpendingService;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettings;
import com.finovara.financeservice.sharedaccount.settings.expense.analysis.dto.ExpenseAnalysisMode;
import com.finovara.financeservice.sharedaccount.settings.expense.analysis.service.ExpenseAnalysisService;
import com.finovara.financeservice.sharedaccount.settings.expense.largeexpense.service.LargeExpenseNotificationService;
import com.finovara.financeservice.sharedaccount.settings.expense.spendcontrol.service.SpendControlService;
import com.finovara.financeservice.sharedaccount.wallet.model.SharedWallet;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.sharedaccount.SharedUsernameResolver;
import com.finovara.financeservice.util.sharedaccount.settings.SharedSettingsManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedExpenseService {

    private final SharedExpenseRepository sharedExpenseRepository;
    private final SharedWalletService sharedWalletService;
    private final SharedLimitRepository sharedLimitRepository;
    private final SharedLimitSpendingService sharedLimitSpendingService;
    private final SharedAccountParticipantsService sharedAccountParticipantsService;
    private final SpendControlService spendControlService;
    private final ExpenseAnalysisService expenseAnalysisService;
    private final LargeExpenseNotificationService largeExpenseNotificationService;
    private final SharedExpenseMapper sharedExpenseMapper;
    private final SharedUsernameResolver usernameResolver;
    private final OutboxService outboxService;

    @Value("${finance.pagination.page-size}")
    private int pageSize;

    @Transactional
    public SharedExpenseResponse addExpense(SharedExpenseRequest request, Long userId) {
        ExpenseCategory category = request.sharedExpenseDto().category();
        BigDecimal amount = request.sharedExpenseDto().amount();
        validateAmount(amount);

        SharedAccountParticipantsResponse participants = sharedAccountParticipantsService.getParticipants(userId);
        String createdByUsername = usernameResolver.resolve(List.of(userId)).get(userId);

        expenseAnalysisService.handleExpenseAnalysis(userId, request.confirmPasswordDto(), amount, ExpenseAnalysisMode.ADD);

        sharedWalletService.lockWalletForUpdate(userId);

        spendControlService.handleSpendControl(userId, amount);

        LocalDate today = LocalDate.now();
        List<SharedLimit> limits = sharedLimitRepository.findAllByUserId(userId);
        validateLimits(participants, limits, today, today, category, amount, null);

        sharedWalletService.removeBalanceFromWallet(userId, amount);

        SharedExpense saved = sharedExpenseRepository.save(SharedExpense.builder()
                .amount(amount)
                .category(category)
                .createdAt(today)
                .description(request.sharedExpenseDto().description())
                .ownerId(participants.ownerId())
                .memberId(participants.memberId())
                .createdByUserId(userId)
                .build());

        logActivity(participants, userId, saved.getId(), SharedAccountActivityLogType.EXPENSE_CREATED);
        largeExpenseNotificationService.handleLargeNotification(userId, saved);

        return new SharedExpenseResponse(saved.getId(), userId, createdByUsername);
    }

    @Transactional
    public Long editExpense(SharedExpenseRequest request, Long userId, Long expenseId) {
        ExpenseCategory category = request.sharedExpenseDto().category();
        BigDecimal amount = request.sharedExpenseDto().amount();
        validateAmount(amount);

        SharedAccountParticipantsResponse participants = sharedAccountParticipantsService.getParticipants(userId);
        expenseAnalysisService.handleExpenseAnalysis(userId, request.confirmPasswordDto(), amount, ExpenseAnalysisMode.EDIT);

        sharedWalletService.lockWalletForUpdate(userId);
        SharedExpense existing = getSharedExpenseByUserIdOrThrow(expenseId, userId);

        BigDecimal oldAmount = existing.getAmount();

        LocalDate today = LocalDate.now();
        List<SharedLimit> limits = sharedLimitRepository.findAllByUserId(userId);
        validateLimits(participants, limits, today, existing.getCreatedAt(), category, amount, existing);

        applyWalletDelta(userId, oldAmount, amount);

        existing.setAmount(amount);
        existing.setCategory(category);
        existing.setDescription(request.sharedExpenseDto().description());

        logActivity(participants, userId, expenseId, SharedAccountActivityLogType.EXPENSE_EDITED);

        return expenseId;
    }

    @Transactional(readOnly = true)
    public List<SharedExpenseDto> getExpense(Long userId, int page) {
        List<SharedExpense> expenses = sharedExpenseRepository.findAllByOwnerIdOrMemberId(userId, PageRequest.of(page, pageSize));

        Map<Long, String> usernameById = usernameResolver.resolve(
                expenses.stream().map(SharedExpense::getCreatedByUserId).collect(Collectors.toSet()));

        return expenses.stream()
                .map(expense -> sharedExpenseMapper.mapToDto(expense, usernameById.get(expense.getCreatedByUserId())))
                .toList();
    }

    @Transactional
    public void deleteExpense(Long expenseId, Long userId) {
        sharedWalletService.lockWalletForUpdate(userId);
        SharedExpense expense = getSharedExpenseByUserIdOrThrow(expenseId, userId);

        sharedWalletService.addBalanceToWallet(userId, expense.getAmount());
        sharedExpenseRepository.delete(expense);

        logActivity(sharedAccountParticipantsService.getParticipants(userId), userId, expenseId, SharedAccountActivityLogType.EXPENSE_DELETED);
    }


    private void applyWalletDelta(Long userId, BigDecimal oldAmount, BigDecimal newAmount) {
        BigDecimal delta = newAmount.subtract(oldAmount);
        if (delta.signum() > 0) {
            sharedWalletService.removeBalanceFromWallet(userId, delta);
        } else if (delta.signum() < 0) {
            sharedWalletService.addBalanceToWallet(userId, delta.negate());
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ONE) < 0) {
            throw new InvalidInputException("Expense amount must be positive");
        }
    }

    private void validateLimits(SharedAccountParticipantsResponse participants, List<SharedLimit> limits, LocalDate today,
                                LocalDate expenseDate, ExpenseCategory newCategory, BigDecimal newAmount, SharedExpense oldExpense) {
        if (limits.isEmpty()) {
            return;
        }
        Map<Long, BigDecimal> spentByLimitId = sharedLimitSpendingService.calculateSpent(participants, limits, today);

        for (SharedLimit limit : limits) {
            if (!isCountedInLimit(limit, newCategory, expenseDate, today)) {
                continue;
            }

            BigDecimal oldAmountInLimit = oldAmountCountedInLimit(limit, oldExpense, today);
            BigDecimal spent = spentByLimitId.getOrDefault(limit.getId(), BigDecimal.ZERO);
            BigDecimal totalAfterChange = spent.subtract(oldAmountInLimit).add(newAmount);

            if (totalAfterChange.compareTo(limit.getAmount()) > 0) {
                throw new MissingRequirementException(limit.getCategory() == null ? "General limit exceeded" : "Category limit exceeded");
            }
        }
    }

    private boolean isCountedInLimit(SharedLimit limit, ExpenseCategory category, LocalDate expenseDate, LocalDate today) {
        boolean categoryMatches = limit.getCategory() == null || limit.getCategory() == category;
        boolean inCurrentPeriod = !expenseDate.isBefore(limit.getPeriodType().getStartDate(today));
        return categoryMatches && inCurrentPeriod;
    }

    private BigDecimal oldAmountCountedInLimit(SharedLimit limit, SharedExpense oldExpense, LocalDate today) {
        if (oldExpense == null) {
            return BigDecimal.ZERO;
        }
        boolean counted = isCountedInLimit(limit, oldExpense.getCategory(), oldExpense.getCreatedAt(), today);
        return counted ? oldExpense.getAmount() : BigDecimal.ZERO;
    }

    private void logActivity(SharedAccountParticipantsResponse participants, Long userId, Long expenseId, SharedAccountActivityLogType type) {
        outboxService.save("SharedAccountExpense", expenseId.toString(), "shared-account.activity",
                new SharedAccountActivityLogEvent(participants.ownerId(), participants.memberId(), userId, expenseId, type, LocalDateTime.now()));
    }

    private SharedExpense getSharedExpenseByUserIdOrThrow(Long expenseId, Long callerId) {
        return sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(expenseId, callerId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Expense not found"));
    }
}