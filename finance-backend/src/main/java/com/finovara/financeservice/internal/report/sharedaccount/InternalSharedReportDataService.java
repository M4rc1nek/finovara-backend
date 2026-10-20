package com.finovara.financeservice.internal.report.sharedaccount;

import com.finovara.contracts.mainaccount.report.financial.dto.DailyCashDto;
import com.finovara.contracts.mainaccount.report.financial.dto.HighestExpenseDto;
import com.finovara.contracts.mainaccount.report.financial.dto.HighestRevenueDto;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.internal.report.util.dto.ExpenseCategorySumDto;
import com.finovara.financeservice.internal.report.util.dto.RevenueCategorySumDto;
import com.finovara.financeservice.sharedaccount.expense.repository.SharedExpenseRepository;
import com.finovara.financeservice.sharedaccount.revenue.repository.SharedRevenueRepository;
import com.finovara.financeservice.sharedaccount.wallet.model.SharedWallet;
import com.finovara.financeservice.sharedaccount.wallet.repository.SharedWalletRepository;
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
public class InternalSharedReportDataService {

    private final SharedExpenseRepository expenseRepository;
    private final SharedRevenueRepository revenueRepository;
    private final SharedWalletRepository sharedWalletRepository;

    public List<HighestExpenseDto> highestExpenses(Long ownerId, Long memberId, LocalDate from, LocalDate to, int pageSize) {
        return expenseRepository.findHighestExpensesByOwnerIdOrMemberIdAndPeriod(ownerId, memberId, from, to, PageRequest.of(0, pageSize));
    }

    public List<HighestRevenueDto> highestRevenues(Long ownerId, Long memberId, LocalDate from, LocalDate to, int pageSize) {
        return revenueRepository.findHighestRevenuesByOwnerIdOrMemberIdAndPeriod(ownerId, memberId, from, to, PageRequest.of(0, pageSize));
    }

    public Map<ExpenseCategory, BigDecimal> expenseSumsByCategory(Long ownerId, Long memberId, LocalDate from, LocalDate to) {
        return expenseRepository.sumExpensesGroupedByCategoryForOwnerIdOrMemberId(ownerId, memberId, from, to).stream()
                .collect(Collectors.toMap(ExpenseCategorySumDto::category, ExpenseCategorySumDto::amount));
    }

    public Map<RevenueCategory, BigDecimal> revenueSumsByCategory(Long ownerId, Long memberId, LocalDate from, LocalDate to) {
        return revenueRepository.sumRevenuesGroupedByCategoryForOwnerIdOrMemberId(ownerId, memberId, from, to).stream()
                .collect(Collectors.toMap(RevenueCategorySumDto::category, RevenueCategorySumDto::amount));
    }

    public List<DailyCashDto> expensesGroupedByDate(Long ownerId, Long memberId) {
        return expenseRepository.sumExpensesGroupedByDateForOwnerIdOrMemberId(ownerId, memberId);
    }

    public List<DailyCashDto> expensesAvgGroupedByDate(Long ownerId, Long memberId) {
        return expenseRepository.avgExpensesGroupedByDateForOwnerIdOrMemberId(ownerId, memberId);
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
