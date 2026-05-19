package com.finovara.finovarabackend.report.smartreport.service.handler;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.smartreport.model.SmartReportType;
import com.finovara.finovarabackend.report.smartreport.service.SmartReportHandler;
import com.finovara.finovarabackend.report.smartreport.service.loader.SmartReportTemplateService;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import com.finovara.finovarabackend.util.percentage.CalculatePercentage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SavingsRateHandler implements SmartReportHandler {

    private final ExpenseRepository expenseRepository;
    private final RevenueRepository revenueRepository;

    private final SmartReportTemplateService templateService;

    @Override
    public SmartReportType getType() {
        return SmartReportType.SAVINGS_RATE;
    }

    @Override
    public String generate(Long userId) {
        BigDecimal totalRevenues = Optional.ofNullable(revenueRepository.sumAllRevenuesByUserAssignedId(userId)).orElse(BigDecimal.ZERO);

        BigDecimal totalExpenses = Optional.ofNullable(expenseRepository.sumAllExpensesByUserAssignedId(userId)).orElse(BigDecimal.ZERO);

        BigDecimal savings;

        if (totalRevenues.compareTo(BigDecimal.ZERO) == 0) {
            savings = BigDecimal.ZERO;
        } else {
            savings = CalculatePercentage.calculatePercentage(totalRevenues.subtract(totalExpenses), totalRevenues);
        }


        String template = templateService.getRandomResponse(SmartReportType.SAVINGS_RATE);
        return template.replace("{amount}", savings.toString());
    }
}
