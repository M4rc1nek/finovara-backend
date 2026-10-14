package com.finovara.reportservice.report.smartreport.service.loader;

import com.finovara.reportservice.report.smartreport.model.SmartReportType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class SmartReportTemplateService {

    private static final String PLACEHOLDER = "{amount}";

    private final SmartReportResourceLoader resourceLoader;
    private final Map<SmartReportType, List<String>> templates = new EnumMap<>(SmartReportType.class);

    @PostConstruct
    void init() {
        templates.put(SmartReportType.MONTH_SPENDING, resourceLoader.readLines("smartreport/answer/MonthSpending"));
        templates.put(SmartReportType.AVERAGE_DAY_SPENDING, resourceLoader.readLines("smartreport/answer/AverageDaySpending"));
        templates.put(SmartReportType.EXPENSE_RATE, resourceLoader.readLines("smartreport/answer/ExpenseRate"));
        templates.put(SmartReportType.SAVINGS_RATE, resourceLoader.readLines("smartreport/answer/SavingsRate"));
    }

    public String render(SmartReportType type, BigDecimal value) {
        String formatted = value.setScale(2, RoundingMode.HALF_UP).toPlainString();
        return getRandomTemplate(type).replace(PLACEHOLDER, formatted);
    }

    private String getRandomTemplate(SmartReportType type) {
        List<String> responses = templates.get(type);
        if (responses == null || responses.isEmpty()) {
            return "Responses are null or empty";
        }
        return responses.get(ThreadLocalRandom.current().nextInt(responses.size()));
    }
}