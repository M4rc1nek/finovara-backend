package com.finovara.finovarabackend.report.smartreport.service;

import com.finovara.finovarabackend.report.smartreport.model.SmartReportType;
import com.finovara.finovarabackend.report.smartreport.service.loader.SmartReportQuestionService;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SmartReportService {

    private final UserManagerService userManagerService;
    private final SmartReportQuestionService smartReportQuestionService;

    private final Map<SmartReportType, SmartReportHandler> handlers;

    public SmartReportService(List<SmartReportHandler> handlerList, UserManagerService userManagerService, SmartReportQuestionService smartReportQuestionService) {
        this.userManagerService = userManagerService;
        this.smartReportQuestionService = smartReportQuestionService;
        this.handlers = handlerList.stream().collect(Collectors.toMap(
                SmartReportHandler::getType, handler -> handler));
    }

    public String generateResponse(Long userId, String userQuestion) {
        User user = userManagerService.getUserByIdOrThrow(userId);

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
