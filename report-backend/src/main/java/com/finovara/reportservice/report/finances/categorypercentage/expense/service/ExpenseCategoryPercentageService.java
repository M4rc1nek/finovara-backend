package com.finovara.reportservice.report.finances.categorypercentage.expense.service;

import com.finovara.contracts.model.PeriodType;
import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.percentage.CalculatePercentage;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.report.finances.categorypercentage.expense.dto.ExpenseCategoryPercentageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExpenseCategoryPercentageService {

    private final FinanceBackendReportClient reportClient;


    @Cacheable(value = "report:expensePercentageByCategory", key = "#userId + ':' + #category + ':' + #periodType")
    public ExpenseCategoryPercentageDto getExpensePercentageByCategoryReport(Long userId, ExpenseCategory category, PeriodType periodType) {

        LocalDate to = LocalDate.now();
        LocalDate from = periodType.getStartDate(to);

        BigDecimal total = reportClient.sumExpenses(userId, from, to);
        BigDecimal inCategory = reportClient.expensesByCategory(userId, from, to, category);
        BigDecimal percentage = CalculatePercentage.calculatePercentage(inCategory, total);

        return new ExpenseCategoryPercentageDto(percentage, category);
    }
}