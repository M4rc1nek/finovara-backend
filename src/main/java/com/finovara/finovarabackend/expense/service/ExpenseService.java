package com.finovara.finovarabackend.expense.service;

import com.finovara.finovarabackend.accountactivity.expense.model.ExpenseActivityType;
import com.finovara.finovarabackend.accountactivity.expense.service.ExpenseActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.expense.dto.ExpenseDTO;
import com.finovara.finovarabackend.expense.dto.ExpenseRequestDto;
import com.finovara.finovarabackend.expense.exception.notfound.ExpenseNotFoundException;
import com.finovara.finovarabackend.expense.mapper.ExpenseMapper;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.limit.exception.unprocessablecontent.LimitExceededException;
import com.finovara.finovarabackend.limit.model.LimitType;
import com.finovara.finovarabackend.limit.repository.LimitRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.finances.expense.controlamount.service.ControlAmountService;
import com.finovara.finovarabackend.usersetting.finances.expense.countlimit.service.CountQuantityLimitService;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.dto.SmartScanMode;
import com.finovara.finovarabackend.usersetting.finances.expense.smartscan.service.SmartScanService;
import com.finovara.finovarabackend.usersetting.finances.revenue.scoring.service.RevenueScoringService;
import com.finovara.finovarabackend.usersetting.piggybank.autopayments.model.AutoPaymentsMode;
import com.finovara.finovarabackend.usersetting.piggybank.roundup.service.RoundUpService;
import com.finovara.finovarabackend.util.model.PeriodType;
import com.finovara.finovarabackend.util.service.expense.ExpenseManagerService;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import com.finovara.finovarabackend.wallet.service.WalletService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final LimitRepository limitRepository;
    private final WalletService walletService;
    private final ExpenseActivityService expenseActivityService;
    private final RoundUpService roundUpService;
    private final CountQuantityLimitService countQuantityLimitService;
    private final ControlAmountService controlAmountService;
    private final SmartScanService smartScanService;
    private final ExpenseManagerService expenseManagerService;
    private final UserManagerService userManagerService;
    private final ExpenseMapper expenseMapper;
    private final FinancialPeriodService financialPeriodService;
    private final RevenueScoringService revenueScoringService;

    @Transactional
    public Long addExpense(ExpenseRequestDto expenseRequestDto, String email, LimitType limitType) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        validateLimitOrThrow(user.getId(), limitType, BigDecimal.ZERO, expenseRequestDto.expenseDTO().amount());

        countQuantityLimitService.calculateCountQuantityLimit(email, expenseRequestDto.countQuantityLimitDto(),
                expenseRequestDto.countQuantityLimitDto().countQuantityLimitStrategy(), expenseRequestDto.confirmPasswordDto());

        Expense expense = Expense.builder()
                .amount(expenseRequestDto.expenseDTO().amount())
                .category(expenseRequestDto.expenseDTO().category())
                .createdAt(LocalDate.now())
                .description(expenseRequestDto.expenseDTO().description())
                .userAssigned(user)
                .build();

        if (expenseRequestDto.expenseDTO().amount().compareTo(BigDecimal.ONE) < 0) {
            throw new InvalidInputException("Expense amount must be positive");
        }
        expenseActivityService.createExpenseActivity(email, ExpenseActivityType.ADDED_EXPENSE, expense);

        smartScanService.handleSmartScan(email, expenseRequestDto.confirmPasswordDto(), expenseRequestDto.expenseDTO().amount(), SmartScanMode.ADD);

        walletService.removeBalanceFromWallet(email, expense.getAmount());
        expenseRepository.save(expense);
        revenueScoringService.recalculateScore(email);

        roundUpService.handleExpenseForRoundUp(email, expense.getId(), AutoPaymentsMode.APPLY);

        controlAmountService.handleExpenseAmountControl(email, expense.getAmount());

        return expense.getId();
    }

    @Transactional
    public Long editExpense(ExpenseRequestDto expenseRequestDto, String email, Long expenseId, LimitType limitType) {
        Expense existingExpense = expenseManagerService.getExpenseByIdOrThrow(expenseId);
        User user = userManagerService.getUserByEmailOrThrow(email);

        if (!existingExpense.getUserAssigned().getId().equals(user.getId())) {
            throw new ExpenseNotFoundException("Expense not found for this user");
        }

        validateLimitOrThrow(user.getId(), limitType, existingExpense.getAmount(), expenseRequestDto.expenseDTO().amount());

        walletService.addBalanceToWallet(email, existingExpense.getAmount());
        walletService.removeBalanceFromWallet(email, expenseRequestDto.expenseDTO().amount());
        roundUpService.handleExpenseForRoundUp(email, expenseId, AutoPaymentsMode.ROLLBACK);

        BigDecimal oldAmount = existingExpense.getAmount();
        ExpenseCategory oldCategory = existingExpense.getCategory();

        existingExpense.setAmount(expenseRequestDto.expenseDTO().amount());
        existingExpense.setCategory(expenseRequestDto.expenseDTO().category());
        existingExpense.setDescription(expenseRequestDto.expenseDTO().description());

        expenseActivityService.updateExpenseActivity(email, ExpenseActivityType.EDITED_EXPENSE, existingExpense, oldAmount, oldCategory);

        smartScanService.handleSmartScan(email, expenseRequestDto.confirmPasswordDto(), expenseRequestDto.expenseDTO().amount(), SmartScanMode.EDIT);

        expenseRepository.save(existingExpense);
        revenueScoringService.recalculateScore(email);

        roundUpService.handleExpenseForRoundUp(email, expenseId, AutoPaymentsMode.APPLY);

        controlAmountService.handleExpenseAmountControl(email, expenseRequestDto.expenseDTO().amount());

        return expenseId;

    }

    public List<ExpenseDTO> getExpense(String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        List<Expense> expenses = expenseRepository.findAllByUserAssignedId(user.getId());

        return expenses.stream()
                .map(expenseMapper::mapExpenseToDTO)
                .toList();
    }

    @Transactional
    public void deleteExpense(Long expenseId, String email) {
        User user = userManagerService.getUserByEmailOrThrow(email);
        Expense expense = expenseRepository.findByIdAndUserAssignedId(expenseId, user.getId())
                .orElseThrow(() -> new ExpenseNotFoundException("Expense not found"));
        roundUpService.handleExpenseForRoundUp(email, expenseId, AutoPaymentsMode.ROLLBACK);
        walletService.addBalanceToWallet(email, expense.getAmount());
        expenseActivityService.createExpenseActivity(email, ExpenseActivityType.DELETED_EXPENSE, expense);
        expenseRepository.delete(expense);
        revenueScoringService.recalculateScore(email);

    }

    private BigDecimal checkSpentInPeriod(LimitType limitType, Long userId) {
        if (limitType == null) return BigDecimal.ZERO;
        return switch (limitType) {
            case DAILY -> financialPeriodService.getExpensesSum(userId, PeriodType.DAILY);
            case WEEKLY -> financialPeriodService.getExpensesSum(userId, PeriodType.WEEKLY);
            case MONTHLY -> financialPeriodService.getExpensesSum(userId, PeriodType.MONTHLY);
        };
    }

    private void validateLimitOrThrow(Long userId, LimitType limitType, BigDecimal existingAmount, BigDecimal newAmount) {
        Optional<BigDecimal> limitAmount = (limitType == null) ? Optional.empty() : limitRepository.getLimitAmountByUserIdAndType(userId, limitType);

        BigDecimal spent = checkSpentInPeriod(limitType, userId);
        BigDecimal totalAmount = spent.subtract(existingAmount).add(newAmount);

        if (limitAmount.isPresent() && totalAmount.compareTo(limitAmount.get()) > 0) {
            throw new LimitExceededException("Limit Exceeded");
        }
    }

}
