package com.finovara.finovarabackend.report.smartreport.service.handler;

import com.finovara.finovarabackend.expense.repository.ExpenseRepository;
import com.finovara.finovarabackend.report.smartreport.model.SmartReportType;
import com.finovara.finovarabackend.report.smartreport.service.SmartReportHandler;
import com.finovara.finovarabackend.report.smartreport.service.loader.SmartReportTemplateService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MonthSpendingHandler implements SmartReportHandler {

    private final ExpenseRepository expenseRepository;
    private final SmartReportTemplateService templateService;

    @Override
    public SmartReportType getType() {
        return SmartReportType.MONTH_SPENDING;
    }

    @Override
    public String generate(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startMonth = today.withDayOfMonth(1);

        BigDecimal sumExpenses = expenseRepository.sumExpensesByUserAndDateRange(userId, startMonth, today).orElse(BigDecimal.ZERO);

        String template = templateService.getRandomResponse(SmartReportType.MONTH_SPENDING);
        return template.replace("{amount}", sumExpenses.toString());
    }
}