package com.finovara.reportservice.report.smartreport.service.loader;

import com.finovara.contracts.exception.serviceunavailable.ServiceUnavailableException;
import com.finovara.reportservice.report.smartreport.model.SmartReportType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class SmartReportTemplateService {

    private final Map<SmartReportType, List<String>> templates = new HashMap<>();
    private final Random random = new Random();

    @PostConstruct
    public void init() {
        loadTemplates(SmartReportType.MONTH_SPENDING, "smartreport/answer/MonthSpending");
        loadTemplates(SmartReportType.AVERAGE_DAY_SPENDING, "smartreport/answer/AverageDaySpending");
        loadTemplates(SmartReportType.EXPENSE_RATE, "smartreport/answer/ExpenseRate");
        loadTemplates(SmartReportType.SAVINGS_RATE, "smartreport/answer/SavingsRate");
    }

    public String getRandomResponse(SmartReportType type) {
        List<String> responses = templates.get(type);

        if (responses == null || responses.isEmpty()) {
            return "Responses are null or empty";
        }

        return responses.get(random.nextInt(responses.size()));
    }

    private void loadTemplates(SmartReportType type, String path) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path)) {

            if (inputStream == null) {
                throw new IllegalStateException("File not found:  " + path);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            List<String> lines = reader.lines()
                    .filter(line -> !line.isBlank())
                    .toList();

            templates.put(type, lines);

        } catch (Exception e) {
            throw new ServiceUnavailableException("Loading error: " + path, e);
        }
    }
}