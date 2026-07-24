package com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.expense.service;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.percentage.CalculatePercentage;
import com.finovara.reportservice.feignclient.FinanceBackendSharedReportClient;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.expense.dto.SharedExpenseCategoryPercentageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SharedExpenseCategoryPercentageService {

    private final FinanceBackendSharedReportClient reportClient;

    @Cacheable(value = "report:sharedExpensePercentageByCategory", key = "#ownerId + ':' + #memberId + ':' + #category + ':' + #periodType")
    public SharedExpenseCategoryPercentageDto getExpensePercentageByCategoryReport(Long ownerId, Long memberId, ExpenseCategory category, PeriodType periodType) {

        LocalDate to = LocalDate.now();
        LocalDate from = periodType.getStartDate(to);

        BigDecimal total = reportClient.sumExpenses(ownerId, memberId, from, to);
        BigDecimal inCategory = reportClient.expensesByCategory(ownerId, memberId, from, to, category);
        BigDecimal percentage = CalculatePercentage.calculatePercentage(inCategory, total);

        return new SharedExpenseCategoryPercentageDto(percentage, category);
    }
}