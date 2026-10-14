package com.finovara.reportservice.report.smartreport.service.loader;

import com.finovara.reportservice.report.smartreport.model.SmartReportType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SmartReportQuestionService {

    private static final String[] QUESTION_FILES = {
            "smartreport/question/MonthSpendingQuestion",
            "smartreport/question/AverageDaySpendingQuestion",
            "smartreport/question/ExpenseRateQuestion",
            "smartreport/question/SavingsRateQuestion"
    };

    private final SmartReportResourceLoader resourceLoader;
    private final Map<String, SmartReportType> questionMap = new HashMap<>();

    @PostConstruct
    void init() {
        for (String file : QUESTION_FILES) {
            resourceLoader.readLines(file).forEach(this::register);
        }
    }

    public SmartReportType getTypeFromQuestion(String userQuestion) {
        return userQuestion == null ? null : questionMap.get(normalize(userQuestion));
    }

    private void register(String line) {
        String[] parts = line.split("\\|");
        if (parts.length == 2) {
            questionMap.put(normalize(parts[1]), SmartReportType.valueOf(parts[0].trim()));
        }
    }

    private String normalize(String text) {
        String lower = text.toLowerCase(Locale.ROOT).replace('ł', 'l');
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("?", "")
                .trim();
    }
}