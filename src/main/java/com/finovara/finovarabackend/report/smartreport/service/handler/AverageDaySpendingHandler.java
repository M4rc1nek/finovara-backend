package com.finovara.finovarabackend.report.smartreport.service.handler;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.smartreport.model.SmartReportType;
import com.finovara.finovarabackend.report.smartreport.service.SmartReportHandler;
import com.finovara.finovarabackend.report.smartreport.service.loader.SmartReportTemplateService;
import com.finovara.finovarabackend.util.service.periodbalance.FinancialPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AverageDaySpendingHandler  implements SmartReportHandler {

    private final ExpenseRepository expenseRepository;
    private final SmartReportTemplateService templateService;
    private final FinancialPeriodService financialPeriodService;

    @Override
    public SmartReportType getType() {
        return SmartReportType.AVERAGE_DAY_SPENDING;
    }

    @Override
    public String generate(Long userId) {
        LocalDate today = financialPeriodService.today();

        LocalDate startMonth = today.withDayOfMonth(1);
        long days = ChronoUnit.DAYS.between(startMonth, today) + 1;

        BigDecimal sumExpenses = Optional.ofNullable(expenseRepository.sumAllExpensesByUserAssignedId(userId))
                        .orElse(BigDecimal.ZERO);

        BigDecimal averageExpenses =
                sumExpenses.divide(BigDecimal.valueOf(days), RoundingMode.HALF_UP);

        String template = templateService.getRandomResponse(SmartReportType.AVERAGE_DAY_SPENDING);
        return template.replace("{amount}", averageExpenses.toString());
    }
}