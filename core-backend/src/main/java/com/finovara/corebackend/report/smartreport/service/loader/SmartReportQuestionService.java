package com.finovara.corebackend.report.smartreport.service.loader;

import com.finovara.corebackend.exception.serviceunavailable.ServiceUnavailableException;
import com.finovara.corebackend.report.smartreport.model.SmartReportType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SmartReportQuestionService {

    private final Map<String, SmartReportType> questionMap = new HashMap<>();

    @PostConstruct
    public void init() {
        loadQuestions("smartreport/question/MonthSpendingQuestion");
        loadQuestions("smartreport/question/AverageDaySpendingQuestion");
        loadQuestions("smartreport/question/ExpenseRateQuestion");
        loadQuestions("smartreport/question/SavingsRateQuestion");
    }

    public SmartReportType getTypeFromQuestion(String userQuestion) {
        return questionMap.get(normalize(userQuestion));
    }

    private void loadQuestions(String path) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path)) {

            if (inputStream == null) {
                throw new IllegalStateException("File not found: " + path);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            // // Builds lookup map (normalized question -> SmartReportType) from classpath resource files
            reader.lines()
                    .filter(line -> !line.isBlank())
                    .forEach(line -> {
                        String[] parts = line.split("\\|");
                        if (parts.length == 2) {
                            String enumName = parts[0].trim();
                            String question = normalize(parts[1]);
                            questionMap.put(question, SmartReportType.valueOf(enumName));
                        }
                    });
        } catch (Exception exception) {
            throw new ServiceUnavailableException("Failed to load Questions", exception);
        }
    }

    private String normalize(String text) {
        return text.toLowerCase()
                .trim()
                .replace("ą", "a")
                .replace("ę", "e")
                .replace("ł", "l")
                .replace("ś", "s")
                .replace("ó", "o")
                .replace("ż", "z")
                .replace("ź", "z")
                .replace("ć", "c")
                .replace("ń", "n")
                .replace("?", "");
    }
}