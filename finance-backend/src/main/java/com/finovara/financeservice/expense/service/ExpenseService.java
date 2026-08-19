package com.finovara.financeservice.expense.service;

import com.finovara.contracts.datadeletable.UserDataDeletable;
import com.finovara.contracts.activity.event.expense.ExpenseActivityEvent;
import com.finovara.contracts.notification.event.limit.LimitStatsEvent;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.contracts.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.contracts.model.activity.ExpenseActivityType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.outbox.OutboxService;
import com.finovara.financeservice.expense.dto.ExpenseDto;
import com.finovara.financeservice.expense.dto.ExpenseRequestDto;
import com.finovara.financeservice.expense.mapper.ExpenseMapper;
import com.finovara.financeservice.expense.model.Expense;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.limit.dto.LimitStatsDto;
import com.finovara.financeservice.limit.model.Limit;
import com.finovara.financeservice.limit.repository.LimitRepository;
import com.finovara.financeservice.limit.service.LimitCalculateService;
import com.finovara.financeservice.settings.finances.expense.controlamount.service.ControlAmountService;
import com.finovara.financeservice.settings.finances.expense.quantitylimit.service.CountQuantityLimitService;
import com.finovara.financeservice.settings.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.financeservice.settings.finances.expense.smartscan.service.SmartScanService;
import com.finovara.financeservice.settings.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.financeservice.settings.piggybank.roundup.service.RoundUpService;
import com.finovara.financeservice.util.expense.ExpenseManagerService;
import com.finovara.financeservice.util.periodbalance.FinancialPeriodService;
import com.finovara.financeservice.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService implements UserDataDeletable {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutboxService outboxService;
    private final ExpenseRepository expenseRepository;
    private final LimitRepository limitRepository;
    private final LimitCalculateService limitCalculateService;
    private final WalletService walletService;
    private final RoundUpService roundUpService;
    private final CountQuantityLimitService countQuantityLimitService;
    private final ControlAmountService controlAmountService;
    private final SmartScanService smartScanService;
    private final ExpenseManagerService expenseManagerService;
    private final ExpenseMapper expenseMapper;
    private final FinancialPeriodService financialPeriodService;
    private final AuthBackendClient authBackendClient;
    private final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;

    @Transactional
    public Long addExpense(ExpenseRequestDto expenseRequestDto, Long userId) {
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(expenseRequestDto.confirmAuthorizationCodeDto()));
        validateLimitOrThrow(userId, expenseRequestDto.expenseDto().category(), expenseRequestDto.expenseDto().category(),
                BigDecimal.ZERO, expenseRequestDto.expenseDto().amount());

        countQuantityLimitService.handleExpenseLimitExceeded(userId, expenseRequestDto.countQuantityLimitDto(),
                expenseRequestDto.countQuantityLimitDto().periodType(), expenseRequestDto.confirmPasswordDto());

        Expense expense = Expense.builder()
                .amount(expenseRequestDto.expenseDto().amount())
                .category(expenseRequestDto.expenseDto().category())
                .createdAt(LocalDate.now())
                .description(expenseRequestDto.expenseDto().description())
                .userId(userId)
                .build();

        if (expenseRequestDto.expenseDto().amount().compareTo(BigDecimal.ONE) < 0) {
            throw new InvalidInputException("Expense amount must be positive");
        }

        smartScanService.handleSmartScan(userId, expenseRequestDto.confirmPasswordDto(), expenseRequestDto.expenseDto().amount(), SmartScanMode.ADD);

        walletService.removeBalanceFromWallet(userId, expense.getAmount());
        expenseRepository.save(expense);

        outboxService.save("Expense", expense.getId().toString(), "activity.expense",
                new ExpenseActivityEvent(userId, ExpenseActivityType.ADDED_EXPENSE, expense.getAmount(), expense.getCategory(), null, null, LocalDateTime.now()));

        roundUpService.handleExpenseForRoundUp(userId, expense.getId(), PiggyBankAutomationMode.APPLY);
        controlAmountService.handleExpenseAmountControl(userId, expense.getAmount());
        publishLimitStatsEvents(userId);

        return expense.getId();
    }

    @Transactional
    public Long editExpense(ExpenseRequestDto expenseRequestDto, Long userId, Long expenseId) {
        Expense existingExpense = expenseManagerService.getExpenseByIdOrThrow(expenseId);
        if (!existingExpense.getUserId().equals(userId)) {
            throw new RequestedEntityNotFoundException("Expense not found for this user");
        }
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(expenseRequestDto.confirmAuthorizationCodeDto()));
        validateLimitOrThrow(userId, existingExpense.getCategory(), expenseRequestDto.expenseDto().category(),
                existingExpense.getAmount(), expenseRequestDto.expenseDto().amount());

        walletService.addBalanceToWallet(userId, existingExpense.getAmount());
        walletService.removeBalanceFromWallet(userId, expenseRequestDto.expenseDto().amount());
        roundUpService.handleExpenseForRoundUp(userId, expenseId, PiggyBankAutomationMode.ROLLBACK);

        BigDecimal oldAmount = existingExpense.getAmount();
        ExpenseCategory oldCategory = existingExpense.getCategory();

        existingExpense.setAmount(expenseRequestDto.expenseDto().amount());
        existingExpense.setCategory(expenseRequestDto.expenseDto().category());
        existingExpense.setDescription(expenseRequestDto.expenseDto().description());

        expenseRepository.save(existingExpense);

        outboxService.save("Expense", expenseId.toString(), "activity.expense",
                new ExpenseActivityEvent(userId, ExpenseActivityType.EDITED_EXPENSE, existingExpense.getAmount(), existingExpense.getCategory(), oldAmount, oldCategory, LocalDateTime.now()));

        smartScanService.handleSmartScan(userId, expenseRequestDto.confirmPasswordDto(), expenseRequestDto.expenseDto().amount(), SmartScanMode.EDIT);
        roundUpService.handleExpenseForRoundUp(userId, expenseId, PiggyBankAutomationMode.APPLY);
        controlAmountService.handleExpenseAmountControl(userId, expenseRequestDto.expenseDto().amount());
        publishLimitStatsEvents(userId);

        return expenseId;
    }

    public List<ExpenseDto> getExpense(Long userId) {
        List<Expense> expenses = expenseRepository.findAllByUserId(userId);

        return expenses.stream()
                .map(expenseMapper::mapExpenseToDto)
                .toList();
    }

    @Transactional
    public void deleteExpense(Long expenseId, Long userId, String authorizationCode) {
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Expense not found"));
        authBackendClient.confirmAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(authorizationCode));
        roundUpService.handleExpenseForRoundUp(userId, expenseId, PiggyBankAutomationMode.ROLLBACK);
        walletService.addBalanceToWallet(userId, expense.getAmount());
        outboxService.save("Expense", expenseId.toString(), "activity.expense",
                new ExpenseActivityEvent(userId, ExpenseActivityType.DELETED_EXPENSE, expense.getAmount(), expense.getCategory(), null, null, LocalDateTime.now()));
        expenseRepository.delete(expense);
        publishLimitStatsEvents(userId);
    }

    private void publishLimitStatsEvents(Long userId) {
        limitRepository.findAllByUserId(userId).forEach(limit -> {
            LimitStatsDto stats = limitCalculateService.calculateLimitStats(limit, userId, LocalDate.now());
            kafkaTemplate.send("limit.calculate-stats", new LimitStatsEvent(
                    userId, stats.limitId(), stats.percentage(), stats.periodType()));
        });
    }

    private void validateLimitOrThrow(Long userId, ExpenseCategory oldCategory, ExpenseCategory newCategory,
                                      BigDecimal oldAmount, BigDecimal newAmount) {

        limitRepository.findAllByUserId(userId).forEach(limit -> {

            if (!limitApplies(limit, newCategory)) {
                return;
            }

            BigDecimal spentAlready = financialPeriodService.getExpensesSum(userId, limit.getPeriodType(), limit.getCategory());
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

    private boolean limitApplies(Limit limit, ExpenseCategory category) {
        return limit.getCategory() == null || limit.getCategory().equals(category);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        expenseRepository.deleteByUserId(userId);
        log.info("Deleted expenses for userId={}", userId);
    }
}