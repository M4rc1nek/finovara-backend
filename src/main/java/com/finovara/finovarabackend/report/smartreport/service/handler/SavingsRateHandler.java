package com.finovara.finovarabackend.report.smartreport.service.handler;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.smartreport.model.SmartReportType;
import com.finovara.finovarabackend.report.smartreport.service.SmartReportHandler;
import com.finovara.finovarabackend.report.smartreport.service.loader.SmartReportTemplateService;
import com.finovara.finovarabackend.revenue.repository.RevenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        if(totalRevenues.compareTo(BigDecimal.ZERO) == 0){
            savings = BigDecimal.ZERO;
        } else {
            savings = totalRevenues.subtract(totalExpenses)
                    .divide(totalRevenues, 2,RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }


        String template = templateService.getRandomResponse(SmartReportType.SAVINGS_RATE);
        return template.replace("{amount}", savings.toString());
    }
}
