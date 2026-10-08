package com.finovara.financeservice.sharedaccount.expense.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.sharedaccount.SharedAccountActivityLogType;
import com.finovara.contracts.sharedaccount.event.activity.SharedAccountActivityLogEvent;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseDto;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseRequest;
import com.finovara.financeservice.sharedaccount.expense.dto.SharedExpenseResponse;
import com.finovara.financeservice.sharedaccount.expense.mapper.SharedExpenseMapper;
import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.limit.model.SharedLimit;
import com.finovara.financeservice.sharedaccount.limit.repository.SharedLimitRepository;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsResponse;
import com.finovara.financeservice.sharedaccount.participants.SharedAccountParticipantsService;
import com.finovara.financeservice.sharedaccount.settings.expense.analysis.dto.ExpenseAnalysisMode;
import com.finovara.financeservice.sharedaccount.settings.expense.analysis.service.ExpenseAnalysisService;
import com.finovara.financeservice.sharedaccount.settings.expense.largeexpense.service.LargeExpenseNotificationService;
import com.finovara.financeservice.sharedaccount.settings.expense.spendcontrol.service.SpendControlService;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import com.finovara.financeservice.util.periodbalance.FinancialPeriodService;
import com.finovara.financeservice.util.transaction.expense.SharedExpenseManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final FinancialPeriodService financialPeriodService;
    private final SharedExpenseManagerService sharedExpenseManagerService;
    private final SharedAccountParticipantsService sharedAccountParticipantsService;
    private final SpendControlService spendControlService;
    private final ExpenseAnalysisService expenseAnalysisService;
    private final LargeExpenseNotificationService largeExpenseNotificationService;
    private final SharedExpenseMapper sharedExpenseMapper;
    private final AuthBackendClient authBackendClient;
    private final OutboxService outboxService;

    @Transactional
    public SharedExpenseResponse addExpense(SharedExpenseRequest sharedExpenseRequest, Long userId) {
        ExpenseCategory category = sharedExpenseRequest.sharedExpenseDto().category();
        BigDecimal amount = sharedExpenseRequest.sharedExpenseDto().amount();
        String description = sharedExpenseRequest.sharedExpenseDto().description();

        if (amount.compareTo(BigDecimal.ONE) < 0) {
            throw new InvalidInputException("Expense amount must be positive");
        }

        spendControlService.handleSpendControl(userId, amount);
        validateLimitOrThrow(userId, category, category, BigDecimal.ZERO, amount);
        expenseAnalysisService.handleExpenseAnalysis(userId, sharedExpenseRequest.confirmPasswordDto(), amount, ExpenseAnalysisMode.ADD);

        SharedAccountParticipantsResponse sharedAccountParticipantsResponse = sharedAccountParticipantsService.getParticipants(userId);
        String createdByUsername = authBackendClient.getUsername(userId);

        SharedExpense expense = SharedExpense.builder()
                .amount(amount)
                .category(category)
                .createdAt(LocalDate.now())
                .description(description)
                .ownerId(sharedAccountParticipantsResponse.ownerId())
                .memberId(sharedAccountParticipantsResponse.memberId())
                .createdByUserId(userId)
                .build();

        sharedWalletService.removeBalanceFromWallet(userId, expense.getAmount());
        SharedExpense saved = sharedExpenseRepository.save(expense);

        outboxService.save("SharedAccountExpense", saved.getId().toString(), "shared-account.activity",
                new SharedAccountActivityLogEvent(sharedAccountParticipantsResponse.ownerId(), sharedAccountParticipantsResponse.memberId(), userId, saved.getId(), SharedAccountActivityLogType.EXPENSE_CREATED, LocalDateTime.now()));

        largeExpenseNotificationService.handleLargeNotification(userId, expense);

        return new SharedExpenseResponse(expense.getId(), userId, createdByUsername);
    }

    @Transactional
    public Long editExpense(SharedExpenseRequest sharedExpenseRequest, Long userId, Long expenseId) {
        SharedExpense existingExpense = sharedExpenseManagerService.getSharedExpenseOrThrow(expenseId);
        ExpenseCategory category = sharedExpenseRequest.sharedExpenseDto().category();
        BigDecimal amount = sharedExpenseRequest.sharedExpenseDto().amount();
        String description = sharedExpenseRequest.sharedExpenseDto().description();

        if (!isOwnerOrMember(existingExpense, userId)) {
            throw new RequestedEntityNotFoundException("Expense not found for this user");
        }

        validateLimitOrThrow(userId, existingExpense.getCategory(), category,
                existingExpense.getAmount(), amount);
        expenseAnalysisService.handleExpenseAnalysis(userId, sharedExpenseRequest.confirmPasswordDto(), amount, ExpenseAnalysisMode.EDIT);

        sharedWalletService.addBalanceToWallet(userId, existingExpense.getAmount());
        sharedWalletService.removeBalanceFromWallet(userId, amount);

        SharedAccountParticipantsResponse sharedAccountParticipantsResponse = sharedAccountParticipantsService.getParticipants(userId);

        existingExpense.setAmount(amount);
        existingExpense.setCategory(category);
        existingExpense.setDescription(description);

        SharedExpense saved = sharedExpenseRepository.save(existingExpense);

        outboxService.save("SharedAccountExpense", saved.getId().toString(), "shared-account.activity",
                new SharedAccountActivityLogEvent(sharedAccountParticipantsResponse.ownerId(), sharedAccountParticipantsResponse.memberId(), userId, saved.getId(), SharedAccountActivityLogType.EXPENSE_EDITED, LocalDateTime.now()));

        return expenseId;
    }

    public List<SharedExpenseDto> getExpense(Long userId) {
        List<SharedExpense> expenses = sharedExpenseRepository.findAllByOwnerIdOrMemberId(userId);

        Map<Long, String> usernameById = expenses.stream()
                .map(SharedExpense::getCreatedByUserId)
                .distinct()
                .collect(Collectors.toMap(id -> id, authBackendClient::getUsername));

        return expenses.stream()
                .map(e -> sharedExpenseMapper.mapToDto(e, usernameById.get(e.getCreatedByUserId())))
                .toList();
    }

    @Transactional
    public void deleteExpense(Long expenseId, Long userId) {
        SharedExpense expense = sharedExpenseRepository.findByIdAndOwnerIdOrMemberId(expenseId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Expense not found"));
        sharedWalletService.addBalanceToWallet(userId, expense.getAmount());
        SharedAccountParticipantsResponse sharedAccountParticipantsResponse = sharedAccountParticipantsService.getParticipants(userId);

        sharedExpenseRepository.delete(expense);
        outboxService.save("SharedAccountExpense", expense.getId().toString(), "shared-account.activity",
                new SharedAccountActivityLogEvent(sharedAccountParticipantsResponse.ownerId(), sharedAccountParticipantsResponse.memberId(), userId, expense.getId(), SharedAccountActivityLogType.EXPENSE_DELETED, LocalDateTime.now()));
    }

    private void validateLimitOrThrow(Long userId, ExpenseCategory oldCategory, ExpenseCategory newCategory,
                                      BigDecimal oldAmount, BigDecimal newAmount) {

        sharedLimitRepository.findAllByUserId(userId).forEach(limit -> {

            if (!limitApplies(limit, newCategory)) {
                return;
            }

            BigDecimal spentAlready = financialPeriodService.getSharedExpensesSum(userId, limit.getPeriodType(), limit.getCategory());

            boolean oldAmountAlreadyCounted = limitApplies(limit, oldCategory);

            BigDecimal amountToRemove = oldAmountAlreadyCounted ? oldAmount : BigDecimal.ZERO;

            BigDecimal totalAfterEdit = spentAlready
                    .subtract(amountToRemove)
                    .add(newAmount);

            if (totalAfterEdit.compareTo(limit.getAmount()) > 0) {
                String msg = limit.getCategory() == null ? "General limit exceeded" : "Category limit exceeded";
                throw new MissingRequirementException(msg);
            }
        });
    }

    private boolean limitApplies(SharedLimit limit, ExpenseCategory category) {
        return limit.getCategory() == null || limit.getCategory().equals(category);
    }

    private boolean isOwnerOrMember(SharedExpense expense, Long userId) {
        return expense.getOwnerId().equals(userId) || expense.getMemberId().equals(userId);
    }
}