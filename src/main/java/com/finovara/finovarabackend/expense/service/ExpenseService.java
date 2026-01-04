package com.finovara.finovarabackend.expense.service;

import com.finovara.finovarabackend.exception.ExpenseNotFoundException;
import com.finovara.finovarabackend.exception.UserNotFoundException;
import com.finovara.finovarabackend.expense.dto.ExpenseDTO;
import com.finovara.finovarabackend.expense.model.Expense;
import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.wallet.repository.WalletRepository;
import com.finovara.finovarabackend.wallet.service.WalletService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final WalletRepository walletRepository;

    public User getUserEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User Not Found"));
    }

    public Expense getExpenseOrThrow(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ExpenseNotFoundException("Expense not found"));
    }

    @Transactional
    public ExpenseDTO addExpense(ExpenseDTO expenseDTO, String email) {
        User user = getUserEmailOrThrow(email);

        Expense expense = Expense.builder()
                .amount(expenseDTO.amount())
                .category(expenseDTO.category())
                .createdAt(LocalDate.now())
                .description(expenseDTO.description())
                .userAssigned(user)
                .build();

        walletService.removeBalanceFromWallet(email, expense.getAmount());
        expenseRepository.save(expense);

        return new ExpenseDTO(
                expense.getId(),
                user.getId(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getCreatedAt(),
                expense.getDescription()

        );
    }

    @Transactional
    public ExpenseDTO editExpense(ExpenseDTO expenseDTO, String email, Long expenseId) {
        Expense existingExpense = getExpenseOrThrow(expenseId);
        User user = getUserEmailOrThrow(email);

        if (!existingExpense.getUserAssigned().getId().equals(user.getId())) {
            throw new ExpenseNotFoundException("Expense not found for this user");
        }

        walletService.addBalanceToWallet(email, existingExpense.getAmount());
        walletService.removeBalanceFromWallet(email, expenseDTO.amount());

        existingExpense.setAmount(expenseDTO.amount());
        existingExpense.setCategory(expenseDTO.category());
        existingExpense.setCreatedAt(LocalDate.now());
        existingExpense.setDescription(expenseDTO.description());

        expenseRepository.save(existingExpense);

        return new ExpenseDTO(
                existingExpense.getId(),
                existingExpense.getUserAssigned().getId(),
                existingExpense.getAmount(),
                existingExpense.getCategory(),
                existingExpense.getCreatedAt(),
                existingExpense.getDescription()

        );

    }

    public List<ExpenseDTO> getExpense(String email) {
        User user = getUserEmailOrThrow(email);
        List<Expense> expenses = expenseRepository.findAllByUserAssignedId(user.getId());

        return expenses.stream()
                .map(this::mapExpenseToDTO)
                .toList();
    }

    public ExpenseDTO mapExpenseToDTO(Expense expense) {
        return new ExpenseDTO(
                expense.getId(),
                expense.getUserAssigned().getId(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getCreatedAt(),
                expense.getDescription()
        );
    }

    @Transactional
    public void deleteExpense(Long expenseId, String email) {
        User user = getUserEmailOrThrow(email);
        Expense expense = expenseRepository.findByIdAndUserAssignedId(expenseId, user.getId())
                .orElseThrow(() -> new ExpenseNotFoundException("Expense not found"));
        walletService.addBalanceToWallet(email, expense.getAmount());
        expenseRepository.delete(expense);

    }
}
