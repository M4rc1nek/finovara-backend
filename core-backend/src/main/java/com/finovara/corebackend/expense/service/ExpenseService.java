package com.finovara.corebackend.expense.service;

import com.finovara.contracts.event.activity.expense.ExpenseActivityEvent;
import com.finovara.contracts.event.notification.limit.LimitStatsEvent;
import com.finovara.contracts.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.contracts.model.activity.ExpenseActivityType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.expense.dto.ExpenseDto;
import com.finovara.corebackend.expense.dto.ExpenseRequestDto;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.corebackend.expense.mapper.ExpenseMapper;
import com.finovara.corebackend.expense.model.Expense;
import com.finovara.corebackend.expense.repository.ExpenseRepository;
import com.finovara.corebackend.limit.dto.LimitStatsDto;
import com.finovara.corebackend.limit.repository.LimitRepository;
import com.finovara.corebackend.limit.service.LimitCalculateService;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.usersetting.finances.expense.controlamount.service.ControlAmountService;
import com.finovara.corebackend.usersetting.finances.expense.countlimit.service.CountQuantityLimitService;
import com.finovara.corebackend.usersetting.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.corebackend.usersetting.finances.expense.smartscan.service.SmartScanService;
import com.finovara.corebackend.usersetting.piggybank.autopayments.model.PiggyBankAutomationMode;
import com.finovara.corebackend.usersetting.piggybank.roundup.service.RoundUpService;
import com.finovara.corebackend.util.expense.ExpenseManagerService;
import com.finovara.contracts.model.PeriodType;
import com.finovara.corebackend.util.periodbalance.FinancialPeriodService;
import com.finovara.corebackend.util.user.service.UserManagerService;
import com.finovara.corebackend.wallet.service.WalletService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ExpenseRepository expenseRepository;
    private final LimitRepository limitRepository;
    private final LimitCalculateService limitCalculateService;
    private final WalletService walletService;
    private final RoundUpService roundUpService;
    private final CountQuantityLimitService countQuantityLimitService;
    private final ControlAmountService controlAmountService;
    private final SmartScanService smartScanService;
    private final ExpenseManagerService expenseManagerService;
    private final UserManagerService userManagerService;
    private final ExpenseMapper expenseMapper;
    private final FinancialPeriodService financialPeriodService;

    @Transactional
    public Long addExpense(ExpenseRequestDto expenseRequestDto, Long userId, PeriodType periodType) {
        User user = userManagerService.getUserByIdOrThrow(userId);

        validateLimitOrThrow(user.getId(), periodType, BigDecimal.ZERO, expenseRequestDto.expenseDto().amount());

        countQuantityLimitService.handleExpenseLimitExceeded(userId, expenseRequestDto.countQuantityLimitDto(),
                expenseRequestDto.countQuantityLimitDto().periodType(), expenseRequestDto.confirmPasswordDto());

        Expense expense = Expense.builder()
                .amount(expenseRequestDto.expenseDto().amount())
                .category(expenseRequestDto.expenseDto().category())
                .createdAt(LocalDate.now())
                .description(expenseRequestDto.expenseDto().description())
                .userAssigned(user)
                .build();

        if (expenseRequestDto.expenseDto().amount().compareTo(BigDecimal.ONE) < 0) {
            throw new InvalidInputException("Expense amount must be positive");
        }

        kafkaTemplate.send("activity.expense", new ExpenseActivityEvent(userId, ExpenseActivityType.ADDED_EXPENSE, expense.getAmount(),
                expense.getCategory(), null, null, LocalDateTime.now()));

        smartScanService.handleSmartScan(userId, expenseRequestDto.confirmPasswordDto(), expenseRequestDto.expenseDto().amount(), SmartScanMode.ADD);

        walletService.removeBalanceFromWallet(userId, expense.getAmount());
        expenseRepository.save(expense);

        roundUpService.handleExpenseForRoundUp(userId, expense.getId(), PiggyBankAutomationMode.APPLY);
        controlAmountService.handleExpenseAmountControl(userId, expense.getAmount());

        publishLimitStatsEvents(userId);

        return expense.getId();
    }

    @Transactional
    public Long editExpense(ExpenseRequestDto expenseRequestDto, Long userId, Long expenseId, PeriodType periodType) {
        Expense existingExpense = expenseManagerService.getExpenseByIdOrThrow(expenseId);
        User user = userManagerService.getUserByIdOrThrow(userId);

        if (!existingExpense.getUserAssigned().getId().equals(user.getId())) {
            throw new RequestedEntityNotFoundException("Expense not found for this user");
        }

        validateLimitOrThrow(user.getId(), periodType, existingExpense.getAmount(), expenseRequestDto.expenseDto().amount());

        walletService.addBalanceToWallet(userId, existingExpense.getAmount());
        walletService.removeBalanceFromWallet(userId, expenseRequestDto.expenseDto().amount());
        roundUpService.handleExpenseForRoundUp(userId, expenseId, PiggyBankAutomationMode.ROLLBACK);

        BigDecimal oldAmount = existingExpense.getAmount();
        ExpenseCategory oldCategory = existingExpense.getCategory();

        existingExpense.setAmount(expenseRequestDto.expenseDto().amount());
        existingExpense.setCategory(expenseRequestDto.expenseDto().category());
        existingExpense.setDescription(expenseRequestDto.expenseDto().description());

        kafkaTemplate.send("activity.expense", new ExpenseActivityEvent(userId, ExpenseActivityType.EDITED_EXPENSE, existingExpense.getAmount(),
                existingExpense.getCategory(), oldAmount, oldCategory, LocalDateTime.now()));

        smartScanService.handleSmartScan(userId, expenseRequestDto.confirmPasswordDto(), expenseRequestDto.expenseDto().amount(), SmartScanMode.EDIT);

        expenseRepository.save(existingExpense);

        roundUpService.handleExpenseForRoundUp(userId, expenseId, PiggyBankAutomationMode.APPLY);
        controlAmountService.handleExpenseAmountControl(userId, expenseRequestDto.expenseDto().amount());

        publishLimitStatsEvents(userId);

        return expenseId;
    }

    public List<ExpenseDto> getExpense(Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        List<Expense> expenses = expenseRepository.findAllByUserAssignedId(user.getId());

        return expenses.stream()
                .map(expenseMapper::mapExpenseToDto)
                .toList();
    }

    @Transactional
    public void deleteExpense(Long expenseId, Long userId) {
        User user = userManagerService.getUserByIdOrThrow(userId);
        Expense expense = expenseRepository.findByIdAndUserAssignedId(expenseId, user.getId())
                .orElseThrow(() -> new RequestedEntityNotFoundException("Expense not found"));
        roundUpService.handleExpenseForRoundUp(userId, expenseId, PiggyBankAutomationMode.ROLLBACK);
        walletService.addBalanceToWallet(userId, expense.getAmount());
        kafkaTemplate.send("activity.expense", new ExpenseActivityEvent(userId, ExpenseActivityType.DELETED_EXPENSE,
                expense.getAmount(), expense.getCategory(), null, null, LocalDateTime.now()));
        expenseRepository.delete(expense);

        publishLimitStatsEvents(userId);
    }

    private void publishLimitStatsEvents(Long userId) {
        limitRepository.findAllByUserAssignedId(userId).forEach(limit -> {
            LimitStatsDto stats = limitCalculateService.calculateLimitStats(limit, userId, LocalDate.now());
            kafkaTemplate.send("limit.calculate-stats", new LimitStatsEvent(
                    userId, stats.limitId(), stats.percentage(), stats.periodType()));
        });
    }

    private BigDecimal checkSpentInPeriod(PeriodType periodType, Long userId) {
        if (periodType == null) return BigDecimal.ZERO;
        return financialPeriodService.getExpensesSum(userId, periodType);
    }

    private void validateLimitOrThrow(Long userId, PeriodType periodType, BigDecimal existingAmount, BigDecimal newAmount) {
        Optional<BigDecimal> limitAmount = (periodType == null) ? Optional.empty() : limitRepository.getLimitAmountByUserIdAndType(userId, periodType);

        BigDecimal spent = checkSpentInPeriod(periodType, userId);
        BigDecimal totalAmount = spent.subtract(existingAmount).add(newAmount);

        if (limitAmount.isPresent() && totalAmount.compareTo(limitAmount.get()) > 0) {
            throw new MissingRequirementException("Limit Exceeded");
        }
    }
}