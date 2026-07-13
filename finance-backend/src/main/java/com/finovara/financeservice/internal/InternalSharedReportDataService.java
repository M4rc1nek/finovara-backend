package com.finovara.financeservice.internal;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.contracts.transaction.report.dto.HighestRevenueDto;
import com.finovara.financeservice.sharedaccount.expense.model.SharedExpense;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenue;
import com.finovara.financeservice.sharedaccount.wallet.model.SharedWallet;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenueRepository;
import com.finovara.financeservice.sharedaccount.wallet.repository.SharedWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InternalSharedReportDataService {

    private final SharedExpenseRepository expenseRepository;
    private final SharedRevenueRepository revenueRepository;
    private final SharedWalletRepository sharedWalletRepository;

    public BigDecimal sumExpenses(Long ownerId, Long memberId, LocalDate from, LocalDate to) {
        return expenseRepository.sumExpensesByOwnerIdOrMemberIdAndDateRange(ownerId, memberId, from, to)
                .orElse(BigDecimal.ZERO);
    }

    public List<HighestExpenseDto> highestExpenses(Long ownerId, Long memberId, LocalDate from, LocalDate to, int pageSize) {
        return expenseRepository.findHighestExpensesByOwnerIdOrMemberIdAndPeriod(ownerId, memberId, from, to, PageRequest.of(0, pageSize));
    }

    public BigDecimal expensesByCategory(Long ownerId, Long memberId, LocalDate from, LocalDate to, ExpenseCategory category) {
        return expenseRepository.findAllByOwnerIdOrMemberIdAndCreatedAtBetweenAndCategory(ownerId, memberId, from, to, category)
                .stream()
                .map(SharedExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal sumAllExpenses(Long ownerId, Long memberId) {
        return expenseRepository.sumAllExpensesByOwnerIdOrMemberId(ownerId, memberId);
    }

    public List<DailyCashDto> expensesGroupedByDate(Long ownerId, Long memberId) {
        return expenseRepository.sumExpensesGroupedByDateForOwnerIdOrMemberId(ownerId, memberId);
    }

    public List<DailyCashDto> expensesAvgGroupedByDate(Long ownerId, Long memberId) {
        return expenseRepository.avgExpensesGroupedByDateForOwnerIdOrMemberId(ownerId, memberId);
    }

    public BigDecimal sumRevenues(Long ownerId, Long memberId, LocalDate from, LocalDate to) {
        return revenueRepository.sumRevenuesByOwnerIdOrMemberIdAndDateRange(ownerId, memberId, from, to)
                .orElse(BigDecimal.ZERO);
    }

    public List<HighestRevenueDto> highestRevenues(Long ownerId, Long memberId, LocalDate from, LocalDate to, int pageSize) {
        return revenueRepository.findHighestRevenuesByOwnerIdOrMemberIdAndPeriod(ownerId, memberId, from, to, PageRequest.of(0, pageSize));
    }

    public BigDecimal revenuesByCategory(Long ownerId, Long memberId, LocalDate from, LocalDate to, RevenueCategory category) {
        return revenueRepository.findAllByOwnerIdOrMemberIdAndCreatedAtBetweenAndCategory(ownerId, memberId, from, to, category)
                .stream()
                .map(SharedRevenue::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal sumAllRevenues(Long ownerId, Long memberId) {
        return revenueRepository.sumAllRevenuesByOwnerIdOrMemberId(ownerId, memberId);
    }

    public List<DailyCashDto> revenuesGroupedByDate(Long ownerId, Long memberId) {
        return revenueRepository.sumRevenuesGroupedByDateForOwnerIdOrMemberId(ownerId, memberId);
    }

    public List<DailyCashDto> revenuesAvgGroupedByDate(Long ownerId, Long memberId) {
        return revenueRepository.avgRevenuesGroupedByDateForOwnerIdOrMemberId(ownerId, memberId);
    }

    public BigDecimal walletBalance(Long ownerId, Long memberId) {
        return sharedWalletRepository.findByOwnerIdOrMemberId(ownerId, memberId)
                .map(SharedWallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }
}