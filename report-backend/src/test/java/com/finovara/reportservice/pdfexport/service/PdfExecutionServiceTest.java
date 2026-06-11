package com.finovara.reportservice.pdfexport.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.model.PeriodType;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.document.PdfReportDocumentFactory;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfExecutionServiceTest {

    private static final Long USER_ID = 1L;
    private static final PeriodType PERIOD_TYPE = PeriodType.MONTHLY;
    private static final String TITLE = "title";
    private static final byte[] PDF_BYTES = {1, 2, 3};

    @Mock
    private PdfReportDocumentFactory factory;

    @Mock
    private ReportPdfHandler strategy;

    @Mock
    private PdfReportDocument document;

    private PdfExecutionService service;

    @BeforeEach
    void setUp() {
        when(strategy.getType()).thenReturn(PdfReportType.SUM_FINANCES);
        service = new PdfExecutionService(factory, List.of(strategy));
    }

    @Nested
    class Execute {

        @Test
        void shouldReturnPdfBytesFromDocument() throws Exception {
            when(strategy.getTitle(PERIOD_TYPE)).thenReturn(TITLE);
            when(factory.create(TITLE)).thenReturn(document);
            when(document.toByteArray()).thenReturn(PDF_BYTES);

            byte[] result = service.execute(PdfReportType.SUM_FINANCES, PERIOD_TYPE, USER_ID);

            assertThat(result).isEqualTo(PDF_BYTES);
            verify(strategy).generate(document, USER_ID, PERIOD_TYPE);
            verify(document).close();
        }

        @Test
        void shouldThrowWhenPeriodTypeIsNull() {
            assertThrows(InvalidInputException.class,
                    () -> service.execute(PdfReportType.SUM_FINANCES, null, USER_ID));
        }

        @Test
        void shouldThrowWhenReportTypeIsNull() {
            assertThrows(InvalidInputException.class,
                    () -> service.execute(null, PERIOD_TYPE, USER_ID));
        }

        @Test
        void shouldThrowWhenStrategyIsMissing() {
            PdfExecutionService localService = new PdfExecutionService(factory, List.of(strategy));

            assertThrows(InvalidInputException.class,
                    () -> localService.execute(PdfReportType.AVERAGE_FINANCES, PERIOD_TYPE, USER_ID));
        }

        @Test
        void shouldWrapIOExceptionInIllegalStateException() throws Exception {
            when(strategy.getTitle(any())).thenReturn(TITLE);
            when(factory.create(any())).thenReturn(document);
            when(document.toByteArray()).thenThrow(new IOException());

            assertThrows(IllegalStateException.class,
                    () -> service.execute(PdfReportType.SUM_FINANCES, PERIOD_TYPE, USER_ID));
        }
    }

    @Nested
    class GetFileName {

        @Test
        void shouldDelegateToStrategy() {
            when(strategy.getFileName(PeriodType.DAILY)).thenReturn("file.pdf");

            String result = service.getFileName(PdfReportType.SUM_FINANCES, PeriodType.DAILY);

            assertThat(result).isEqualTo("file.pdf");
        }

        @Test
        void shouldThrowWhenReportTypeIsNull() {
            assertThrows(InvalidInputException.class,
                    () -> service.getFileName(null, PeriodType.DAILY));
        }

        @Test
        void shouldThrowWhenPeriodTypeIsNull() {
            assertThrows(InvalidInputException.class,
                    () -> service.getFileName(PdfReportType.SUM_FINANCES, null));
        }
    }
}
