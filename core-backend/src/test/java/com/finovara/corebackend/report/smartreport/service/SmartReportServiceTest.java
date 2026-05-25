package com.finovara.corebackend.report.smartreport.service;

import com.finovara.corebackend.report.smartreport.model.SmartReportType;
import com.finovara.corebackend.report.smartreport.service.loader.SmartReportQuestionService;
import com.finovara.corebackend.user.model.User;
import com.finovara.corebackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartReportServiceTest {

    @Mock
    private UserManagerService userManagerService;

    @Mock
    private SmartReportQuestionService smartReportQuestionService;

    @Mock
    private SmartReportHandler handler;

    private static final Long USER_ID = 1L;
    User user;

    @BeforeEach
    void setUp(){
        user = new User();
        user.setId(USER_ID);
    }

    @Test
    void shouldReturnResponseFromHandler() {
        String question = "Ile wydałem w tym miesiącu?";

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

        when(smartReportQuestionService.getTypeFromQuestion(question)).thenReturn(SmartReportType.MONTH_SPENDING);

        when(handler.getType()).thenReturn(SmartReportType.MONTH_SPENDING);
        when(handler.generate(1L)).thenReturn("50.00");

        SmartReportService smartReportService = new SmartReportService(
                List.of(handler),
                userManagerService,
                smartReportQuestionService
        );

        String result = smartReportService.generateResponse(USER_ID, question);

        assertEquals("50.00", result);

        verify(userManagerService).getUserByIdOrThrow(USER_ID);
        verify(smartReportQuestionService).getTypeFromQuestion(question);
        verify(handler).getType();
    }

    @Test
    void shouldReturnDefaultMessageWhenTypeIsNull() {
        String question = "unknown question";

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);
        when(smartReportQuestionService.getTypeFromQuestion(question)).thenReturn(null);

        SmartReportService smartReportService = new SmartReportService(
                List.of(handler),
                userManagerService,
                smartReportQuestionService
        );

        String result = smartReportService.generateResponse(USER_ID, question);

        assertEquals("Nie  jestem aż tak inteligenty aby odpowiedzieć na to pytanie, zadaj pytanie z księgi pytań!", result);

        verify(userManagerService).getUserByIdOrThrow(USER_ID);
        verify(smartReportQuestionService).getTypeFromQuestion(question);
    }

    @Test
    void shouldReturnUnsupportedMessageWhenHandlerNotFound() {
        String question = "Ile wydałem w tym miesiącu?";

        when(userManagerService.getUserByIdOrThrow(USER_ID)).thenReturn(user);

        when(smartReportQuestionService.getTypeFromQuestion(question)).thenReturn(SmartReportType.MONTH_SPENDING);

        SmartReportService smartReportService = new SmartReportService(
                List.of(),
                userManagerService,
                smartReportQuestionService
        );

        String result = smartReportService.generateResponse(USER_ID, question);

        assertEquals("Report type not supported", result);

        verify(userManagerService).getUserByIdOrThrow(USER_ID);
        verify(smartReportQuestionService).getTypeFromQuestion(question);
    }
}