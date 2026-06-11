package com.finovara.corebackend.internal.service;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import com.finovara.contracts.transaction.report.dto.DailyCashDto;
import com.finovara.contracts.transaction.report.dto.HighestExpenseDto;
import com.finovara.contracts.transaction.report.dto.HighestRevenueDto;
import com.finovara.corebackend.expense.model.Expense;
import com.finovara.corebackend.expense.repository.ExpenseRepository;
import com.finovara.corebackend.revenue.model.Revenue;
import com.finovara.corebackend.revenue.repository.RevenueRepository;
import com.finovara.corebackend.wallet.model.Wallet;
import com.finovara.corebackend.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InternalReportDataService {

    //Move this service to transaction-service

    private final ExpenseRepository expenseRepository;
    private final RevenueRepository revenueRepository;
    private final WalletRepository walletRepository;

    public BigDecimal sumExpenses(Long userId, LocalDate from, LocalDate to) {
        return expenseRepository.sumExpensesByUserAndDateRange(userId, from, to)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal avgExpenses(Long userId, LocalDate from, LocalDate to) {
        return expenseRepository.avgExpensesByUserAssignedIdAndPeriod(userId, from, to)
                .orElse(BigDecimal.ZERO);
    }

    public List<HighestExpenseDto> highestExpenses(Long userId, LocalDate from, LocalDate to, int pageSize) {
        return expenseRepository.findHighestExpensesByUserAssignedIdAndPeriod(userId, from, to, PageRequest.of(0, pageSize));
    }

    public BigDecimal expensesByCategory(Long userId, LocalDate from, LocalDate to, ExpenseCategory category) {
        return expenseRepository.findAllByUserAssignedIdAndCreatedAtBetweenAndCategory(userId, from, to, category)
                .stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal sumAllExpenses(Long userId) {
        return expenseRepository.sumAllExpensesByUserAssignedId(userId);
    }

    public List<DailyCashDto> expensesGroupedByDate(Long userId) {
        return expenseRepository.sumExpensesGroupedByDate(userId);
    }

    public List<DailyCashDto> expensesAvgGroupedByDate(Long userId) {
        return expenseRepository.avgExpensesGroupedByDate(userId);
    }

    public BigDecimal sumRevenues(Long userId, LocalDate from, LocalDate to) {
        return revenueRepository.sumRevenuesByUserAndDateRange(userId, from, to)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal avgRevenues(Long userId, LocalDate from, LocalDate to) {
        return revenueRepository.avgRevenuesByUserAssignedIdAndPeriod(userId, from, to)
                .orElse(BigDecimal.ZERO);
    }

    public List<HighestRevenueDto> highestRevenues(Long userId, LocalDate from, LocalDate to, int pageSize) {
        return revenueRepository.findHighestRevenuesByUserAssignedIdAndPeriod(userId, from, to, PageRequest.of(0, pageSize));
    }

    public BigDecimal revenuesByCategory(Long userId, LocalDate from, LocalDate to, RevenueCategory category) {
        return revenueRepository.findAllByUserAssignedIdAndCreatedAtBetweenAndCategory(userId, from, to, category)
                .stream()
                .map(Revenue::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal sumAllRevenues(Long userId) {
        return revenueRepository.sumAllRevenuesByUserAssignedId(userId);
    }

    public List<DailyCashDto> revenuesGroupedByDate(Long userId) {
        return revenueRepository.sumRevenuesGroupedByDate(userId);
    }

    public List<DailyCashDto> revenuesAvgGroupedByDate(Long userId) {
        return revenueRepository.avgRevenuesGroupedByDate(userId);
    }

    public BigDecimal walletBalance(Long userId) {
        return walletRepository.findByUserAssignedId(userId)
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }
}


