package com.finovara.financeservice.sharedaccount.service.expense;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import com.finovara.financeservice.sharedaccount.dto.expense.SharedExpenseDto;
import com.finovara.financeservice.sharedaccount.dto.expense.SharedExpenseResponse;
import com.finovara.financeservice.sharedaccount.dto.wallet.SharedWalletDto;
import com.finovara.financeservice.sharedaccount.model.expense.SharedExpense;
import com.finovara.financeservice.sharedaccount.repository.expense.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.service.expense.mapper.SharedExpenseMapper;
import com.finovara.financeservice.sharedaccount.service.wallet.SharedWalletService;
import com.finovara.financeservice.util.expense.SharedExpenseManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharedExpenseService {

    private final SharedExpenseRepository sharedExpenseRepository;
    private final SharedWalletService sharedWalletService;
    private final SharedExpenseManagerService sharedExpenseManagerService;
    private final SharedExpenseMapper sharedExpenseMapper;
    private final AuthBackendClient authBackendClient;

    @Transactional
    public SharedExpenseResponse addExpense(SharedExpenseDto sharedExpenseDto, Long userId) {
        if (sharedExpenseDto.amount().compareTo(BigDecimal.ONE) < 0) {
            throw new InvalidInputException("Expense amount must be positive");
        }
        SharedWalletDto walletDto = sharedWalletService.getWallet(userId);
        String createdByUsername = authBackendClient.getUsername(userId);

        SharedExpense expense = SharedExpense.builder()
                .amount(sharedExpenseDto.amount())
                .category(sharedExpenseDto.category())
                .createdAt(LocalDate.now())
                .description(sharedExpenseDto.description())
                .ownerId(walletDto.ownerId())
                .memberId(walletDto.memberId())
                .createdByUserId(userId)
                .build();

        sharedWalletService.removeBalanceFromWallet(userId, expense.getAmount());
        sharedExpenseRepository.save(expense);

        return new SharedExpenseResponse(expense.getId(), userId, createdByUsername);
    }
    @Transactional
    public Long editExpense(SharedExpenseDto sharedExpenseDto, Long userId, Long expenseId) {
        SharedExpense existingExpense = sharedExpenseManagerService.getSharedExpenseOrThrow(expenseId);

        if (!isOwnerOrMember(existingExpense, userId)) {
            throw new RequestedEntityNotFoundException("Expense not found for this user");
        }

        sharedWalletService.addBalanceToWallet(userId, existingExpense.getAmount());
        sharedWalletService.removeBalanceFromWallet(userId, sharedExpenseDto.amount());

        existingExpense.setAmount(sharedExpenseDto.amount());
        existingExpense.setCategory(sharedExpenseDto.category());
        existingExpense.setDescription(sharedExpenseDto.description());

        sharedExpenseRepository.save(existingExpense);

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
        sharedExpenseRepository.delete(expense);
    }

    private boolean isOwnerOrMember(SharedExpense expense, Long userId) {
        return expense.getOwnerId().equals(userId) || expense.getMemberId().equals(userId);
    }
}