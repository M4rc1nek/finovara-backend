package com.finovara.corebackend.report.smartreport.service;

import com.finovara.corebackend.report.smartreport.model.SmartReportType;
import com.finovara.corebackend.report.smartreport.service.loader.SmartReportQuestionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SmartReportService {

    private final SmartReportQuestionService smartReportQuestionService;

    private final Map<SmartReportType, SmartReportHandler> handlers;

    public SmartReportService(List<SmartReportHandler> handlerList, SmartReportQuestionService smartReportQuestionService) {
        this.smartReportQuestionService = smartReportQuestionService;
        this.handlers = handlerList.stream().collect(Collectors.toMap(
                SmartReportHandler::getType, handler -> handler));
    }

    public String generateResponse(Long userId, String userQuestion) {

        SmartReportType type = smartReportQuestionService.getTypeFromQuestion(userQuestion);

        if (type == null)
            return "Nie  jestem aż tak inteligenty aby odpowiedzieć na to pytanie, zadaj pytanie z księgi pytań!";

        SmartReportHandler handler = handlers.get(type);

        if (handler == null) {
            return "Report type not supported";
        }

        return handler.generate(userId);
    }
}
