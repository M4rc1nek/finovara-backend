package com.finovara.finovarabackend.expense.service;

import com.finovara.finovarabackend.config.TimeConfig;
import com.finovara.finovarabackend.exception.ExpenseNotFoundException;
import com.finovara.finovarabackend.exception.LimitExceededException;
import com.finovara.finovarabackend.exception.UserNotFoundException;
import com.finovara.finovarabackend.expense.dto.ExpenseDTO;
import com.finovara.finovarabackend.expense.mapper.ExpenseMapper;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.limit.model.LimitType;
import com.finovara.finovarabackend.limit.repository.LimitRepository;
import com.finovara.finovarabackend.limit.service.SpentInPeriodService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final LimitRepository limitRepository;
    private final ExpenseMapper expenseMapper;
    private final WalletService walletService;
    private final SpentInPeriodService spentInPeriodService;
    private final TimeConfig timeConfig;

    @Transactional
    public Long addExpense(ExpenseDTO expenseDTO, String email, LimitType limitType) {
        User user = getUserByEmailOrThrow(email);

        validateLimitOrThrow(user.getId(), limitType, BigDecimal.ZERO, expenseDTO.amount());

        Expense expense = Expense.builder()
                .amount(expenseDTO.amount())
                .category(expenseDTO.category())
                .createdAt(LocalDate.now(timeConfig.clock()))
                .description(expenseDTO.description())
                .userAssigned(user)
                .build();

        walletService.removeBalanceFromWallet(email, expense.getAmount());
        expenseRepository.save(expense);

        return expense.getId();
    }

    @Transactional
    public Long editExpense(ExpenseDTO expenseDTO, String email, Long expenseId, LimitType limitType) {
        Expense existingExpense = getExpenseOrThrow(expenseId);
        User user = getUserByEmailOrThrow(email);

        if (!existingExpense.getUserAssigned().getId().equals(user.getId())) {
            throw new ExpenseNotFoundException("Expense not found for this user");
        }

        validateLimitOrThrow(user.getId(), limitType, existingExpense.getAmount(), expenseDTO.amount());

        walletService.addBalanceToWallet(email, existingExpense.getAmount());
        walletService.removeBalanceFromWallet(email, expenseDTO.amount());

        existingExpense.setAmount(expenseDTO.amount());
        existingExpense.setCategory(expenseDTO.category());
        existingExpense.setDescription(expenseDTO.description());

        expenseRepository.save(existingExpense);

        return expenseId;

    }

    public List<ExpenseDTO> getExpense(String email) {
        User user = getUserByEmailOrThrow(email);
        List<Expense> expenses = expenseRepository.findAllByUserAssignedId(user.getId());

        return expenses.stream()
                .map(expenseMapper::mapExpenseToDTO)
                .toList();
    }

    @Transactional
    public void deleteExpense(Long expenseId, String email) {
        User user = getUserByEmailOrThrow(email);
        Expense expense = expenseRepository.findByIdAndUserAssignedId(expenseId, user.getId())
                .orElseThrow(() -> new ExpenseNotFoundException("Expense not found"));
        walletService.addBalanceToWallet(email, expense.getAmount());
        expenseRepository.delete(expense);

    }

    private User getUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User Not Found"));
    }

    private Expense getExpenseOrThrow(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ExpenseNotFoundException("Expense not found"));
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
