package com.finovara.finovarabackend.reports.smartreport.service.handler;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.reports.smartreport.model.SmartReportType;
import com.finovara.finovarabackend.reports.smartreport.service.SmartReportHandler;
import com.finovara.finovarabackend.reports.smartreport.service.loader.SmartReportTemplateService;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExpenseRateHandler implements SmartReportHandler {

    private final ExpenseRepository expenseRepository;
    private final RevenueRepository revenueRepository;

    private final SmartReportTemplateService templateService;

    @Override
    public SmartReportType getType() {
        return SmartReportType.EXPENSE_RATE;
    }

    @Override
    public String generate(Long userId) {
        BigDecimal sumExpenses = Optional.ofNullable(expenseRepository.sumAllExpensesByUserAssignedId(userId)).orElse(BigDecimal.ZERO);
        BigDecimal sumRevenue = Optional.ofNullable(revenueRepository.sumAllRevenuesByUserAssignedId(userId)).orElse(BigDecimal.ZERO);

        BigDecimal total = sumExpenses.divide(sumRevenue, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        String template = templateService.getRandomResponse(SmartReportType.EXPENSE_RATE);
        return template.replace("{amount}", total.toString());

    }

}
