package com.finovara.reportservice.report.smartreport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.finovara.contracts.mainaccount.report.financial.dto.FinancialReportDto;
import com.finovara.contracts.util.PeriodType;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.report.smartreport.model.SmartReportType;
import com.finovara.reportservice.report.smartreport.service.loader.SmartReportQuestionService;
import com.finovara.reportservice.report.smartreport.service.loader.SmartReportTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class SmartReportServiceTest {

    private static final Long USER_ID = 1L;
    private static final String QUESTION = "Ile wydałem w tym miesiącu?";
    private static final BigDecimal VALUE = new BigDecimal("123.45");
    private static final String ANSWER = "Wydałeś 123.45 zł";
    private static final String UNKNOWN_QUESTION_MESSAGE =
            "Nie jestem aż tak inteligentny, aby odpowiedzieć na to pytanie, zadaj pytanie z księgi pytań!";
    private static final String UNSUPPORTED_TYPE_MESSAGE = "Report type not supported";

    @Mock
    private SmartReportHandler monthSpendingHandler;

    @Mock
    private SmartReportHandler savingsRateHandler;

    @Mock
    private SmartReportQuestionService questionService;

    @Mock
    private SmartReportTemplateService templateService;

    @Mock
    private FinanceBackendReportClient reportClient;

    @Mock
    private FinancialReportDto report;

    private SmartReportService service;

    @BeforeEach
    void setUp() {
        when(monthSpendingHandler.getType()).thenReturn(SmartReportType.MONTH_SPENDING);
        when(savingsRateHandler.getType()).thenReturn(SmartReportType.SAVINGS_RATE);
        service = new SmartReportService(
                List.of(monthSpendingHandler, savingsRateHandler), questionService, templateService, reportClient);
    }

    @Nested
    class GenerateResponse {

        @Test
        void shouldReturnRenderedAnswerWhenQuestionAndHandlerExist() {
            when(questionService.getTypeFromQuestion(QUESTION)).thenReturn(SmartReportType.MONTH_SPENDING);
            when(monthSpendingHandler.getPeriodType()).thenReturn(PeriodType.MONTHLY);
            when(reportClient.getFinancialReport(USER_ID, PeriodType.MONTHLY)).thenReturn(report);
            when(monthSpendingHandler.calculate(report)).thenReturn(VALUE);
            when(templateService.render(SmartReportType.MONTH_SPENDING, VALUE)).thenReturn(ANSWER);

            String result = service.generateResponse(USER_ID, QUESTION);

            assertEquals(ANSWER, result);
        }

        @Test
        void shouldUseHandlerPeriodTypeWhenFetchingReport() {
            when(questionService.getTypeFromQuestion(QUESTION)).thenReturn(SmartReportType.SAVINGS_RATE);
            when(savingsRateHandler.getPeriodType()).thenReturn(PeriodType.WEEKLY);
            when(reportClient.getFinancialReport(USER_ID, PeriodType.WEEKLY)).thenReturn(report);
            when(savingsRateHandler.calculate(report)).thenReturn(VALUE);
            when(templateService.render(SmartReportType.SAVINGS_RATE, VALUE)).thenReturn(ANSWER);

            String result = service.generateResponse(USER_ID, QUESTION);

            assertEquals(ANSWER, result);
            verify(reportClient).getFinancialReport(USER_ID, PeriodType.WEEKLY);
        }

        @Test
        void shouldNotCallOtherHandlersWhenHandlerForTypeExists() {
            when(questionService.getTypeFromQuestion(QUESTION)).thenReturn(SmartReportType.MONTH_SPENDING);
            when(monthSpendingHandler.getPeriodType()).thenReturn(PeriodType.MONTHLY);
            when(reportClient.getFinancialReport(USER_ID, PeriodType.MONTHLY)).thenReturn(report);
            when(monthSpendingHandler.calculate(report)).thenReturn(VALUE);
            when(templateService.render(SmartReportType.MONTH_SPENDING, VALUE)).thenReturn(ANSWER);

            service.generateResponse(USER_ID, QUESTION);

            verify(savingsRateHandler, never()).calculate(any());
            verify(savingsRateHandler, never()).getPeriodType();
        }

        @Test
        void shouldPassNullUserIdToClientWhenUserIdIsNull() {
            when(questionService.getTypeFromQuestion(QUESTION)).thenReturn(SmartReportType.MONTH_SPENDING);
            when(monthSpendingHandler.getPeriodType()).thenReturn(PeriodType.MONTHLY);
            when(reportClient.getFinancialReport(null, PeriodType.MONTHLY)).thenReturn(report);
            when(monthSpendingHandler.calculate(report)).thenReturn(VALUE);
            when(templateService.render(SmartReportType.MONTH_SPENDING, VALUE)).thenReturn(ANSWER);

            String result = service.generateResponse(null, QUESTION);

            assertEquals(ANSWER, result);
        }

        @Test
        void shouldReturnUnknownQuestionMessageWhenQuestionTypeIsNotResolved() {
            when(questionService.getTypeFromQuestion(QUESTION)).thenReturn(null);

            String result = service.generateResponse(USER_ID, QUESTION);

            assertEquals(UNKNOWN_QUESTION_MESSAGE, result);
            verifyNoInteractions(reportClient, templateService);
        }

        @Test
        void shouldReturnUnknownQuestionMessageWhenQuestionIsNull() {
            when(questionService.getTypeFromQuestion(null)).thenReturn(null);

            String result = service.generateResponse(USER_ID, null);

            assertEquals(UNKNOWN_QUESTION_MESSAGE, result);
            verifyNoInteractions(reportClient, templateService);
        }

        @Test
        void shouldReturnUnsupportedTypeMessageWhenNoHandlerExistsForType() {
            when(questionService.getTypeFromQuestion(QUESTION)).thenReturn(SmartReportType.EXPENSE_RATE);

            String result = service.generateResponse(USER_ID, QUESTION);

            assertEquals(UNSUPPORTED_TYPE_MESSAGE, result);
            verifyNoInteractions(reportClient, templateService);
        }

        @Test
        void shouldThrowExceptionWhenReportClientFails() {
            when(questionService.getTypeFromQuestion(QUESTION)).thenReturn(SmartReportType.MONTH_SPENDING);
            when(monthSpendingHandler.getPeriodType()).thenReturn(PeriodType.MONTHLY);
            when(reportClient.getFinancialReport(USER_ID, PeriodType.MONTHLY)).thenThrow(new IllegalStateException("client error"));

            assertThrows(IllegalStateException.class, () -> service.generateResponse(USER_ID, QUESTION));

            verifyNoInteractions(templateService);
        }

        @Test
        void shouldThrowExceptionWhenHandlerCalculationFails() {
            when(questionService.getTypeFromQuestion(QUESTION)).thenReturn(SmartReportType.MONTH_SPENDING);
            when(monthSpendingHandler.getPeriodType()).thenReturn(PeriodType.MONTHLY);
            when(reportClient.getFinancialReport(USER_ID, PeriodType.MONTHLY)).thenReturn(report);
            when(monthSpendingHandler.calculate(report)).thenThrow(new ArithmeticException("calculation error"));

            assertThrows(ArithmeticException.class, () -> service.generateResponse(USER_ID, QUESTION));

            verifyNoInteractions(templateService);
        }

        @Test
        void shouldThrowExceptionWhenTemplateRenderingFails() {
            when(questionService.getTypeFromQuestion(QUESTION)).thenReturn(SmartReportType.MONTH_SPENDING);
            when(monthSpendingHandler.getPeriodType()).thenReturn(PeriodType.MONTHLY);
            when(reportClient.getFinancialReport(USER_ID, PeriodType.MONTHLY)).thenReturn(report);
            when(monthSpendingHandler.calculate(report)).thenReturn(VALUE);
            when(templateService.render(SmartReportType.MONTH_SPENDING, VALUE)).thenThrow(new IllegalStateException("render error"));

            assertThrows(IllegalStateException.class, () -> service.generateResponse(USER_ID, QUESTION));
        }
    }

    @Nested
    class HandlerRegistration {

        @Test
        void shouldUseLastRegisteredHandlerWhenTwoHandlersHaveSameType() {
            when(savingsRateHandler.getType()).thenReturn(SmartReportType.MONTH_SPENDING);
            SmartReportService serviceWithDuplicates = new SmartReportService(
                    List.of(monthSpendingHandler, savingsRateHandler), questionService, templateService, reportClient);
            when(questionService.getTypeFromQuestion(QUESTION)).thenReturn(SmartReportType.MONTH_SPENDING);
            when(savingsRateHandler.getPeriodType()).thenReturn(PeriodType.DAILY);
            when(reportClient.getFinancialReport(USER_ID, PeriodType.DAILY)).thenReturn(report);
            when(savingsRateHandler.calculate(report)).thenReturn(VALUE);
            when(templateService.render(SmartReportType.MONTH_SPENDING, VALUE)).thenReturn(ANSWER);

            String result = serviceWithDuplicates.generateResponse(USER_ID, QUESTION);

            assertEquals(ANSWER, result);
            verify(monthSpendingHandler, never()).calculate(any());
        }

        @Test
        void shouldReturnUnsupportedTypeMessageWhenHandlerListIsEmpty() {
            SmartReportService serviceWithoutHandlers = new SmartReportService(
                    Collections.emptyList(), questionService, templateService, reportClient);
            when(questionService.getTypeFromQuestion(QUESTION)).thenReturn(SmartReportType.MONTH_SPENDING);

            String result = serviceWithoutHandlers.generateResponse(USER_ID, QUESTION);

            assertEquals(UNSUPPORTED_TYPE_MESSAGE, result);
        }

        @Test
        void shouldThrowExceptionWhenHandlerListIsNull() {
            assertThrows(NullPointerException.class,
                    () -> new SmartReportService(null, questionService, templateService, reportClient));
        }
    }
}