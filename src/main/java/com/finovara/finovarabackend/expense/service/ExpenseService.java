package com.finovara.finovarabackend.expense.service;

import com.finovara.finovarabackend.config.TimeConfig;
import com.finovara.finovarabackend.exception.ExpenseNotFoundException;
import com.finovara.finovarabackend.exception.InvalidInputException;
import com.finovara.finovarabackend.exception.LimitExceededException;
import com.finovara.finovarabackend.expense.dto.ExpenseDTO;
import com.finovara.finovarabackend.expense.mapper.ExpenseMapper;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.limit.model.LimitType;
import com.finovara.finovarabackend.limit.repository.LimitRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersettings.piggybank.autopayments.model.AutoPaymentsMode;
import com.finovara.finovarabackend.usersettings.piggybank.roundup.service.RoundUpService;
import com.finovara.finovarabackend.util.service.expense.ExpenseManagerService;
import com.finovara.finovarabackend.util.service.time.SpentInPeriodService;
import com.finovara.finovarabackend.util.service.user.UserManagerService;
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
    private final RoundUpService roundUpService;
    private final ExpenseManagerService expenseManagerService;
    private final UserManagerService userManagerService;
    private final ExpenseMapper expenseMapper;
    private final SpentInPeriodService spentInPeriodService;
    private final TimeConfig timeConfig;

    @Transactional
    public Long addExpense(ExpenseDTO expenseDTO, String email, LimitType limitType) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        validateLimitOrThrow(user.getId(), limitType, BigDecimal.ZERO, expenseDTO.amount());

        Expense expense = Expense.builder()
                .amount(expenseDTO.amount())
                .category(expenseDTO.category())
                .createdAt(LocalDate.now(timeConfig.clock()))
                .description(expenseDTO.description())
                .userAssigned(user)
                .build();

        if (expenseDTO.amount().compareTo(BigDecimal.ONE) < 0) {
            throw new InvalidInputException("Expense amount must be positive");
        }

        walletService.removeBalanceFromWallet(email, expense.getAmount());
        expenseRepository.save(expense);

        roundUpService.handleExpenseForRoundUp(email, expense.getId(), AutoPaymentsMode.APPLY);

        return expense.getId();
    }

    @Transactional
    public Long editExpense(ExpenseDTO expenseDTO, String email, Long expenseId, LimitType limitType) {
        Expense existingExpense = expenseManagerService.getExpenseByIdOrThrow(expenseId);
        User user = userManagerService.getUserByEmailOrThrow(email);

        if (!existingExpense.getUserAssigned().getId().equals(user.getId())) {
            throw new ExpenseNotFoundException("Expense not found for this user");
        }

        validateLimitOrThrow(user.getId(), limitType, existingExpense.getAmount(), expenseDTO.amount());

        walletService.addBalanceToWallet(email, existingExpense.getAmount());
        walletService.removeBalanceFromWallet(email, expenseDTO.amount());
        roundUpService.handleExpenseForRoundUp(email, expenseId, AutoPaymentsMode.ROLLBACK);

        existingExpense.setAmount(expenseDTO.amount());
        existingExpense.setCategory(expenseDTO.category());
        existingExpense.setDescription(expenseDTO.description());

        expenseRepository.save(existingExpense);

        roundUpService.handleExpenseForRoundUp(email, expenseId, AutoPaymentsMode.APPLY);

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
        expenseRepository.delete(expense);

    }

    private BigDecimal checkSpentInPeriod(LimitType limitType, Long userId) {
        if (limitType == null) return BigDecimal.ZERO;
        return switch (limitType) {
            case DAILY -> spentInPeriodService.getSpentToday(userId);
            case WEEKLY -> spentInPeriodService.getSpentWeekly(userId);
            case MONTHLY -> spentInPeriodService.getSpentMonthly(userId);
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
