package com.finovara.financeservice.internal.report.mainaccount;

import com.finovara.contracts.mainaccount.report.financial.dto.DailyCashDto;
import com.finovara.contracts.mainaccount.report.financial.dto.HighestExpenseDto;
import com.finovara.contracts.mainaccount.report.financial.dto.HighestRevenueDto;
import com.finovara.contracts.mainaccount.report.savingsplan.dto.SavingsPlanMonthDto;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.contracts.util.model.RevenueCategory;
import com.finovara.financeservice.expense.repository.ExpenseRepository;
import com.finovara.financeservice.internal.report.util.dto.ExpenseCategorySumDto;
import com.finovara.financeservice.internal.report.util.dto.MonthlyExpenseTotalByCategoryDto;
import com.finovara.financeservice.internal.report.util.dto.RevenueCategorySumDto;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import com.finovara.financeservice.wallet.model.Wallet;
import com.finovara.financeservice.wallet.repository.WalletRepository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
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

    @Transactional(readOnly = true)
    public List<SavingsPlanMonthDto> monthlyData(Long userId) {
        log.info("Loading savings plan monthly data for userId={}", userId);
        return revenueRepository.findLatestRevenueDateByUserId(userId)
                .map(latestRevenueDate -> latestMonthData(userId, latestRevenueDate))
                .orElseGet(() -> {
                    log.info("No active revenue found for userId={}", userId);
                    return List.of();
                });
    }

    private List<SavingsPlanMonthDto> latestMonthData(Long userId, LocalDate latestRevenueDate) {
        LocalDate month = latestRevenueDate.withDayOfMonth(1);
        LocalDate lastDayOfMonth = month.plusMonths(1).minusDays(1);
        BigDecimal monthlyRevenue = revenueRepository.sumRevenuesByUserAndDateRange(userId, month, lastDayOfMonth)
                .orElse(BigDecimal.ZERO);
        Map<ExpenseCategory, BigDecimal> monthlyExpenses = new EnumMap<>(ExpenseCategory.class);
        for (MonthlyExpenseTotalByCategoryDto total : expenseRepository.sumMonthlyExpensesByUserId(userId, month, lastDayOfMonth.plusDays(1))) {
            monthlyExpenses.merge(total.category(), total.amount(), BigDecimal::add);
        }
        return List.of(new SavingsPlanMonthDto(month, monthlyRevenue, Map.copyOf(monthlyExpenses)));
    }
}
