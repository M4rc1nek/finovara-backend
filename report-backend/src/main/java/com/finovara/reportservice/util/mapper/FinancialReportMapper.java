package com.finovara.reportservice.util.mapper;

import com.finovara.reportservice.util.dto.financial.FinancialCashFlowDto;
import com.finovara.reportservice.util.dto.financial.FinancialCategoryPercentageDto;
import com.finovara.reportservice.report.finances.calculate.categorypercentage.expense.dto.ExpenseCategoryPercentageDto;
import com.finovara.reportservice.report.finances.calculate.categorypercentage.revenue.dto.RevenueCategoryPercentageDto;
import com.finovara.reportservice.report.finances.calculate.chart.dto.CashFlowDto;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.expense.dto.SharedExpenseCategoryPercentageDto;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.categorypercentage.revenue.dto.SharedRevenueCategoryPercentageDto;
import com.finovara.reportservice.sharedaccount.report.finances.calculate.chart.dto.SharedCashFlowDto;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class FinancialReportMapper {

    public List<FinancialCategoryPercentageDto> mapExpensePercentages(List<ExpenseCategoryPercentageDto> percentages) {
        return percentages.stream()
                .map(dto -> new FinancialCategoryPercentageDto(dto.category().name(), dto.percentage()))
                .toList();
    }

    public List<FinancialCategoryPercentageDto> mapSharedExpensePercentages(List<SharedExpenseCategoryPercentageDto> percentages) {
        return percentages.stream()
                .map(dto -> new FinancialCategoryPercentageDto(dto.category().name(), dto.percentage()))
                .toList();
    }

    public List<FinancialCategoryPercentageDto> mapRevenuePercentages(List<RevenueCategoryPercentageDto> percentages) {
        return percentages.stream()
                .map(dto -> new FinancialCategoryPercentageDto(dto.category().name(), dto.percentage()))
                .toList();
    }

    public List<FinancialCategoryPercentageDto> mapSharedRevenuePercentages(List<SharedRevenueCategoryPercentageDto> percentages) {
        return percentages.stream()
                .map(dto -> new FinancialCategoryPercentageDto(dto.category().name(), dto.percentage()))
                .toList();
    }

    public List<FinancialCashFlowDto> mapCashFlow(List<CashFlowDto> cashFlow) {
        return cashFlow.stream()
                .map(dto -> new FinancialCashFlowDto(dto.revenue(), dto.expense(), dto.date()))
                .toList();
    }

    public List<FinancialCashFlowDto> mapSharedCashFlow(List<SharedCashFlowDto> cashFlow) {
        return cashFlow.stream()
                .map(dto -> new FinancialCashFlowDto(dto.revenue(), dto.expense(), dto.date()))
                .toList();
    }
}