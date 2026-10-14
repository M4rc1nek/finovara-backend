package com.finovara.financeservice.internal.report.mainaccount;

import com.finovara.contracts.mainaccount.report.financial.dto.DailyCashDto;
import com.finovara.contracts.mainaccount.report.financial.dto.HighestExpenseDto;
import com.finovara.contracts.mainaccount.report.financial.dto.HighestRevenueDto;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.internal.report.util.dto.ExpenseCategorySumDto;
import com.finovara.financeservice.internal.report.util.dto.RevenueCategorySumDto;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InternalReportDataService {

    private final ExpenseRepository expenseRepository;
    private final RevenueRepository revenueRepository;
    private final WalletRepository walletRepository;

    public List<HighestExpenseDto> highestExpenses(Long userId, LocalDate from, LocalDate to, int pageSize) {
        return expenseRepository.findHighestExpensesByUserIdAndPeriod(userId, from, to, PageRequest.of(0, pageSize));
    }

    public List<HighestRevenueDto> highestRevenues(Long userId, LocalDate from, LocalDate to, int pageSize) {
        return revenueRepository.findHighestRevenuesByUserIdAndPeriod(userId, from, to, PageRequest.of(0, pageSize));
    }

    public Map<ExpenseCategory, BigDecimal> expenseSumsByCategory(Long userId, LocalDate from, LocalDate to) {
        return expenseRepository.sumExpensesGroupedByCategory(userId, from, to).stream()
                .collect(Collectors.toMap(ExpenseCategorySumDto::category, ExpenseCategorySumDto::amount));
    }

    public Map<RevenueCategory, BigDecimal> revenueSumsByCategory(Long userId, LocalDate from, LocalDate to) {
        return revenueRepository.sumRevenuesGroupedByCategory(userId, from, to).stream()
                .collect(Collectors.toMap(RevenueCategorySumDto::category, RevenueCategorySumDto::amount));
    }

    public List<DailyCashDto> expensesGroupedByDate(Long userId) {
        return expenseRepository.sumExpensesGroupedByDate(userId);
    }

    public List<DailyCashDto> expensesAvgGroupedByDate(Long userId) {
        return expenseRepository.avgExpensesGroupedByDate(userId);
    }

    public List<DailyCashDto> revenuesGroupedByDate(Long userId) {
        return revenueRepository.sumRevenuesGroupedByDate(userId);
    }

    public List<DailyCashDto> revenuesAvgGroupedByDate(Long userId) {
        return revenueRepository.avgRevenuesGroupedByDate(userId);
    }

    public BigDecimal walletBalance(Long userId) {
        return walletRepository.findByUserId(userId)
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }
}