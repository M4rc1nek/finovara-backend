package com.finovara.financeservice.expense.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.contracts.mainaccount.activity.event.expense.ExpenseActivityEvent;
import com.finovara.contracts.mainaccount.activity.model.ExpenseActivityType;
import com.finovara.contracts.mainaccount.transaction.event.limit.LimitStatsEvent;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;
import com.finovara.contracts.user.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.user.datadeletable.UserDataDeletable;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.financeservice.expense.dto.ExpenseDto;
import com.finovara.financeservice.expense.dto.ExpenseRequestDto;
import com.finovara.financeservice.expense.mapper.ExpenseMapper;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.repository.LimitRepository;
import com.finovara.financeservice.limit.service.LimitCalculateService;
import com.finovara.financeservice.limit.service.LimitSpendingService;
import com.finovara.financeservice.riskverification.service.RiskGuardService;
import com.finovara.financeservice.settings.finances.expense.controlamount.service.ControlAmountService;
import com.finovara.financeservice.settings.finances.expense.quantitylimit.service.CountQuantityLimitService;
import com.finovara.financeservice.settings.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.financeservice.settings.finances.expense.smartscan.service.SmartScanService;
import com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.financeservice.settings.piggybank.roundup.service.RoundUpService;
import com.finovara.financeservice.util.transaction.TransactionOrigin;
import com.finovara.financeservice.util.transaction.expense.ExpenseManagerService;
import com.finovara.financeservice.wallet.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService implements UserDataDeletable {

    private static final String LIMIT_STATS_TOPIC = "limit.calculate-stats";

    private final OutboxService outboxService;
    private final ExpenseRepository expenseRepository;
    private final LimitRepository limitRepository;
    private final LimitCalculateService limitCalculateService;
    private final LimitSpendingService limitSpendingService;
    private final WalletService walletService;
    private final RoundUpService roundUpService;
    private final CountQuantityLimitService countQuantityLimitService;
    private final ControlAmountService controlAmountService;
    private final SmartScanService smartScanService;
    private final ExpenseManagerService expenseManagerService;
    private final ExpenseMapper expenseMapper;
    private final AuthBackendClient authBackendClient;
    private final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;
    private final RiskGuardService riskGuardService;

    @Value("${finance.pagination.page-size}")
    private int pageSize;

    @Transactional
    @CacheEvict(value = "expense:suggestion", key = "#userId")
    public Long addExpense(ExpenseRequestDto dto, Long userId, HttpServletRequest servletRequest, TransactionOrigin origin) {
        ExpenseDto expenseDto = dto.expenseDto();
        validateAmount(expenseDto.amount());

        if (origin == TransactionOrigin.USER_MANUAL) {
            riskGuardService.guard(userId, RiskTriggerType.EXPENSE, expenseDto.amount(), expenseDto.category().name(),
                    authBackendClient.getUserEmail(userId), dto.riskVerificationSourceEventId(), servletRequest);
            authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(dto.confirmAuthorizationCodeDto()));
        }

        LocalDate today = LocalDate.now();

        walletService.lockWalletForUpdate(userId);

        List<Limit> limits = limitRepository.findAllActiveByUserId(userId);
        validateLimits(userId, limits, today, today, expenseDto.category(), expenseDto.amount(), null);

        countQuantityLimitService.handleExpenseLimitExceeded(userId, dto.countQuantityLimitDto(),
                dto.countQuantityLimitDto().periodType(), dto.confirmPasswordDto());
        smartScanService.handleSmartScan(userId, dto.confirmPasswordDto(), expenseDto.amount(), SmartScanMode.ADD);

        walletService.removeBalanceFromWallet(userId, expenseDto.amount());

        Expense expense = expenseRepository.save(Expense.builder()
                .amount(expenseDto.amount())
                .category(expenseDto.category())
                .createdAt(today)
                .description(expenseDto.description())
                .userId(userId)
                .build());

        outboxService.save("Expense", expense.getId().toString(), "expense.created",
                new ExpenseActivityEvent(userId, ExpenseActivityType.ADDED_EXPENSE, expense.getAmount(), expense.getCategory(), null, null, LocalDateTime.now()));

        roundUpService.handleExpenseForRoundUp(userId, expense.getId(), PiggyBankAutomationMode.APPLY);
        controlAmountService.handleExpenseAmountControl(userId, expense.getAmount());
        publishLimitStatsEvents(userId, limits, today);

        return expense.getId();
    }

    @Transactional
    @CacheEvict(value = "expense:suggestion", key = "#userId")
    public Long editExpense(ExpenseRequestDto dto, Long userId, Long expenseId, HttpServletRequest servletRequest) {
        ExpenseDto expenseDto = dto.expenseDto();
        validateAmount(expenseDto.amount());

        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(dto.confirmAuthorizationCodeDto()));
        riskGuardService.guard(userId, RiskTriggerType.EXPENSE, expenseDto.amount(), expenseDto.category().name(),
                authBackendClient.getUserEmail(userId), dto.riskVerificationSourceEventId(), servletRequest);

        LocalDate today = LocalDate.now();

        walletService.lockWalletForUpdate(userId);
        Expense existing = expenseManagerService.getExpenseByUserIdOrThrow(expenseId, userId);

        BigDecimal oldAmount = existing.getAmount();
        ExpenseCategory oldCategory = existing.getCategory();

        List<Limit> limits = limitRepository.findAllActiveByUserId(userId);
        validateLimits(userId, limits, today, existing.getCreatedAt(), expenseDto.category(), expenseDto.amount(), existing);

        applyWalletDelta(userId, oldAmount, expenseDto.amount());
        roundUpService.handleExpenseForRoundUp(userId, expenseId, PiggyBankAutomationMode.ROLLBACK);

        existing.setAmount(expenseDto.amount());
        existing.setCategory(expenseDto.category());
        existing.setDescription(expenseDto.description());

        outboxService.save("Expense", expenseId.toString(), "expense.created",
                new ExpenseActivityEvent(userId, ExpenseActivityType.EDITED_EXPENSE, existing.getAmount(), existing.getCategory(), oldAmount, oldCategory, LocalDateTime.now()));

        smartScanService.handleSmartScan(userId, dto.confirmPasswordDto(), expenseDto.amount(), SmartScanMode.EDIT);
        roundUpService.handleExpenseForRoundUp(userId, expenseId, PiggyBankAutomationMode.APPLY);
        controlAmountService.handleExpenseAmountControl(userId, expenseDto.amount());
        publishLimitStatsEvents(userId, limits, today);

        return expenseId;
    }

    @Transactional(readOnly = true)
    public List<ExpenseDto> getExpense(Long userId, int page) {
        return expenseRepository.findAllByUserId(userId, PageRequest.of(page, pageSize)).stream()
                .map(expenseMapper::mapExpenseToDto)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "expense:suggestion", key = "#userId")
    public void deleteExpense(Long expenseId, Long userId, String authorizationCode) {
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(authorizationCode));

        walletService.lockWalletForUpdate(userId);
        Expense expense = expenseManagerService.getExpenseByUserIdOrThrow(expenseId, userId);

        roundUpService.handleExpenseForRoundUp(userId, expenseId, PiggyBankAutomationMode.ROLLBACK);
        walletService.addBalanceToWallet(userId, expense.getAmount());
        outboxService.save("Expense", expenseId.toString(), "expense.created",
                new ExpenseActivityEvent(userId, ExpenseActivityType.DELETED_EXPENSE, expense.getAmount(), expense.getCategory(), null, null, LocalDateTime.now()));
        expenseRepository.delete(expense);

        publishLimitStatsEvents(userId, limitRepository.findAllActiveByUserId(userId), LocalDate.now());
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        List<Expense> expenses = expenseRepository.findAllByUserId(userId);
        expenseRepository.deleteAll(expenses);
        log.info("Deleted expenses for userId={}", userId);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ONE) < 0) {
            throw new InvalidInputException("Expense amount must be positive");
        }
    }

    private void applyWalletDelta(Long userId, BigDecimal oldAmount, BigDecimal newAmount) {
        BigDecimal delta = newAmount.subtract(oldAmount);
        if (delta.signum() > 0) {
            walletService.removeBalanceFromWallet(userId, delta);
        } else if (delta.signum() < 0) {
            walletService.addBalanceToWallet(userId, delta.negate());
        }
    }

    private void validateLimits(Long userId, List<Limit> limits, LocalDate today, LocalDate expenseDate,
                                ExpenseCategory newCategory, BigDecimal newAmount, Expense oldExpense) {
        if (limits.isEmpty()) {
            return;
        }
        Map<Long, BigDecimal> spentByLimitId = limitSpendingService.calculateSpent(userId, limits, today);

        for (Limit limit : limits) {
            if (!isCountedInLimit(limit, newCategory, expenseDate, today)) {
                continue;
            }

            BigDecimal spent = spentByLimitId.get(limit.getId());
            BigDecimal oldAmountInLimit = oldAmountCountedInLimit(limit, oldExpense, today);
            BigDecimal totalAfterChange = spent.subtract(oldAmountInLimit).add(newAmount);

            if (totalAfterChange.compareTo(limit.getAmount()) > 0) {
                throw new MissingRequirementException(limit.getCategory() == null ? "General limit exceeded" : "Category limit exceeded");
            }
        }
    }

    private boolean isCountedInLimit(Limit limit, ExpenseCategory category, LocalDate expenseDate, LocalDate today) {
        boolean categoryMatches = limit.getCategory() == null || limit.getCategory() == category;
        boolean inCurrentPeriod = !expenseDate.isBefore(limit.getPeriodType().getStartDate(today));
        return categoryMatches && inCurrentPeriod;
    }

    private BigDecimal oldAmountCountedInLimit(Limit limit, Expense oldExpense, LocalDate today) {
        if (oldExpense == null) {
            return BigDecimal.ZERO;
        }
        boolean counted = isCountedInLimit(limit, oldExpense.getCategory(), oldExpense.getCreatedAt(), today);
        return counted ? oldExpense.getAmount() : BigDecimal.ZERO;
    }

    private void publishLimitStatsEvents(Long userId, List<Limit> limits, LocalDate today) {
        if (limits.isEmpty()) {
            return;
        }
        limitCalculateService.calculateLimitStats(limits, userId, today).forEach(stats ->
                outboxService.save("Limit", stats.limitId().toString(), LIMIT_STATS_TOPIC,
                        new LimitStatsEvent(userId, stats.limitId(), stats.percentage(), stats.periodType())));
    }
}
