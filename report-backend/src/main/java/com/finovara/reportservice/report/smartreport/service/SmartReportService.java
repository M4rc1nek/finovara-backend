package com.finovara.reportservice.report.smartreport.service;

import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.report.smartreport.model.SmartReportType;
import com.finovara.reportservice.report.smartreport.service.loader.SmartReportQuestionService;
import com.finovara.reportservice.report.smartreport.service.loader.SmartReportTemplateService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class SmartReportService {

    private static final String UNKNOWN_QUESTION =
            "Nie jestem aż tak inteligentny, aby odpowiedzieć na to pytanie, zadaj pytanie z księgi pytań!";
    private static final String UNSUPPORTED_TYPE = "Report type not supported";

    private final SmartReportQuestionService questionService;
    private final SmartReportTemplateService templateService;
    private final FinanceBackendReportClient reportClient;
    private final Map<SmartReportType, SmartReportHandler> handlers = new EnumMap<>(SmartReportType.class);

    public SmartReportService(List<SmartReportHandler> handlerList,
                              SmartReportQuestionService questionService,
                              SmartReportTemplateService templateService,
                              FinanceBackendReportClient reportClient) {
        this.questionService = questionService;
        this.templateService = templateService;
        this.reportClient = reportClient;
        handlerList.forEach(handler -> handlers.put(handler.getType(), handler));
    }

    public String generateResponse(Long userId, String userQuestion) {
        SmartReportType type = questionService.getTypeFromQuestion(userQuestion);
        if (type == null) {
            return UNKNOWN_QUESTION;
        }

        SmartReportHandler handler = handlers.get(type);
        if (handler == null) {
            return UNSUPPORTED_TYPE;
        }

        var report = reportClient.getFinancialReport(userId, handler.getPeriodType());
        BigDecimal value = handler.calculate(report);

        return templateService.render(type, value);
    }
}