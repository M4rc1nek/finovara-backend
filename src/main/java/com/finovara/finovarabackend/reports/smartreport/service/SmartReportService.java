package com.finovara.finovarabackend.reports.smartreport.service;

import com.finovara.finovarabackend.reports.smartreport.model.SmartReportType;
import com.finovara.finovarabackend.reports.smartreport.service.loader.SmartReportQuestionService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.service.user.service.UserManagerService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SmartReportService {

    private final List<SmartReportHandler> handlerList;

    private final UserManagerService userManagerService;
    private final SmartReportQuestionService smartReportQuestionService;

    private  Map<SmartReportType, SmartReportHandler> handlers;
    @PostConstruct
    void init() {
        handlers = handlerList.stream()
                .collect(Collectors.toMap(SmartReportHandler::getType, smartReportHandler -> smartReportHandler));
    }

    public String generateResponse(String email, String userQuestion) {
        User user = userManagerService.getUserByEmailOrThrow(email);

        SmartReportType type = smartReportQuestionService.getTypeFromQuestion(userQuestion);

        if (type == null)
            return "Nie  jestem aż tak inteligenty aby odpowiedzieć na to pytanie, zadaj pytanie z księgi pytań!";

        SmartReportHandler handler = handlers.get(type);

        if (handler == null) {
            return "Report type not supported";
        }

        return handler.generate(user.getId());
    }
}
