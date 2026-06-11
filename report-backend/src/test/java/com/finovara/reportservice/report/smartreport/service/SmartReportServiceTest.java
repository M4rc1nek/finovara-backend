package com.finovara.reportservice.report.smartreport.service;

import com.finovara.reportservice.report.smartreport.model.SmartReportType;
import com.finovara.reportservice.report.smartreport.service.loader.SmartReportQuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartReportServiceTest {

    private static final Long USER_ID = 1L;
    private static final String MONTH_SPENDING_QUESTION = "Ile wydałem w tym miesiącu?";
    private static final String UNKNOWN_QUESTION = "unknown question";
    private static final String FALLBACK_MESSAGE =
            "Nie  jestem aż tak inteligenty aby odpowiedzieć na to pytanie, zadaj pytanie z księgi pytań!";
    private static final String UNSUPPORTED_MESSAGE = "Report type not supported";

    @Mock
    private SmartReportQuestionService questionService;

    @Mock
    private SmartReportHandler monthSpendingHandler;

    private SmartReportService service;

    @BeforeEach
    void setUp() {
        when(monthSpendingHandler.getType()).thenReturn(SmartReportType.MONTH_SPENDING);
        service = new SmartReportService(List.of(monthSpendingHandler), questionService);
    }

    @Nested
    class GenerateResponse {

        @Test
        void shouldDelegateToMatchingHandler() {
            when(questionService.getTypeFromQuestion(MONTH_SPENDING_QUESTION))
                    .thenReturn(SmartReportType.MONTH_SPENDING);
            when(monthSpendingHandler.generate(USER_ID)).thenReturn("50.00");

            String result = service.generateResponse(USER_ID, MONTH_SPENDING_QUESTION);

            assertThat(result).isEqualTo("50.00");
            verify(questionService).getTypeFromQuestion(MONTH_SPENDING_QUESTION);
            verify(monthSpendingHandler).generate(USER_ID);
        }

        @Test
        void shouldReturnFallbackMessageWhenQuestionTypeIsUnknown() {
            when(questionService.getTypeFromQuestion(UNKNOWN_QUESTION)).thenReturn(null);

            String result = service.generateResponse(USER_ID, UNKNOWN_QUESTION);

            assertThat(result).isEqualTo(FALLBACK_MESSAGE);
            verify(questionService).getTypeFromQuestion(UNKNOWN_QUESTION);
        }

        @Test
        void shouldReturnUnsupportedMessageWhenHandlerIsMissing() {
            SmartReportService serviceWithoutHandlers =
                    new SmartReportService(List.of(), questionService);

            when(questionService.getTypeFromQuestion(MONTH_SPENDING_QUESTION))
                    .thenReturn(SmartReportType.MONTH_SPENDING);

            String result = serviceWithoutHandlers.generateResponse(USER_ID, MONTH_SPENDING_QUESTION);

            assertThat(result).isEqualTo(UNSUPPORTED_MESSAGE);
            verify(questionService).getTypeFromQuestion(MONTH_SPENDING_QUESTION);
        }
    }
}
