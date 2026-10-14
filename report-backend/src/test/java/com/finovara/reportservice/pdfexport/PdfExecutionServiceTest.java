package com.finovara.reportservice.pdfexport;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.contracts.util.PeriodType;
import com.finovara.reportservice.feignclient.FinanceBackendReportClient;
import com.finovara.reportservice.pdfexport.document.PdfReportDocument;
import com.finovara.reportservice.pdfexport.document.PdfReportDocumentFactory;
import com.finovara.reportservice.pdfexport.model.PdfReportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfExecutionServiceTest {

    private static final Long USER_ID = 1L;
    private static final String TITLE = "Tytuł raportu";
    private static final String FILE_NAME = "raport-monthly.pdf";
    private static final byte[] PDF_BYTES = {1, 2, 3};

    @Mock
    private PdfReportDocumentFactory documentFactory;

    @Mock
    private FinanceBackendReportClient reportClient;

    @Mock
    private ReportPdfHandler handler;

    @Mock
    private PdfReportDocument document;

    @Captor
    private ArgumentCaptor<PdfReportContext> contextCaptor;

    private PdfExecutionService service;

    @BeforeEach
    void setUp() {
        when(handler.getType()).thenReturn(PdfReportType.SUM_FINANCES);
        service = new PdfExecutionService(documentFactory, reportClient, List.of(handler));
    }

    @Nested
    class Execute {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnPdfBytesWhenPeriodTypeIsSupported(PeriodType periodType) throws IOException {
            when(handler.getTitle(periodType)).thenReturn(TITLE);
            when(documentFactory.create(TITLE)).thenReturn(document);
            when(document.toByteArray()).thenReturn(PDF_BYTES);

            byte[] result = service.execute(PdfReportType.SUM_FINANCES, periodType, USER_ID);

            assertArrayEquals(PDF_BYTES, result);
        }

        @Test
        void shouldCreateDocumentWithHandlerTitleWhenExecuting() throws IOException {
            when(handler.getTitle(PeriodType.MONTHLY)).thenReturn(TITLE);
            when(documentFactory.create(TITLE)).thenReturn(document);
            when(document.toByteArray()).thenReturn(PDF_BYTES);

            service.execute(PdfReportType.SUM_FINANCES, PeriodType.MONTHLY, USER_ID);

            verify(documentFactory).create(TITLE);
        }

        @Test
        void shouldPassDocumentAndContextToHandlerWhenExecuting() throws IOException {
            when(handler.getTitle(PeriodType.WEEKLY)).thenReturn(TITLE);
            when(documentFactory.create(TITLE)).thenReturn(document);
            when(document.toByteArray()).thenReturn(PDF_BYTES);

            service.execute(PdfReportType.SUM_FINANCES, PeriodType.WEEKLY, USER_ID);

            verify(handler).generate(eq(document), contextCaptor.capture());
            assertEquals(PeriodType.WEEKLY, contextCaptor.getValue().periodType());
        }

        @Test
        void shouldCloseDocumentWhenPdfIsGenerated() throws IOException {
            when(handler.getTitle(PeriodType.MONTHLY)).thenReturn(TITLE);
            when(documentFactory.create(TITLE)).thenReturn(document);
            when(document.toByteArray()).thenReturn(PDF_BYTES);

            service.execute(PdfReportType.SUM_FINANCES, PeriodType.MONTHLY, USER_ID);

            verify(document).close();
        }

        @Test
        void shouldCloseDocumentWhenHandlerThrowsIoException() throws IOException {
            when(handler.getTitle(PeriodType.MONTHLY)).thenReturn(TITLE);
            when(documentFactory.create(TITLE)).thenReturn(document);
            doThrow(new IOException("generation failed")).when(handler).generate(eq(document), any(PdfReportContext.class));

            assertThrows(IllegalStateException.class,
                    () -> service.execute(PdfReportType.SUM_FINANCES, PeriodType.MONTHLY, USER_ID));

            verify(document).close();
        }

        @Test
        void shouldCloseDocumentWhenHandlerThrowsRuntimeException() throws IOException {
            when(handler.getTitle(PeriodType.MONTHLY)).thenReturn(TITLE);
            when(documentFactory.create(TITLE)).thenReturn(document);
            doThrow(new IllegalArgumentException("invalid")).when(handler).generate(eq(document), any(PdfReportContext.class));

            assertThrows(IllegalArgumentException.class,
                    () -> service.execute(PdfReportType.SUM_FINANCES, PeriodType.MONTHLY, USER_ID));

            verify(document).close();
        }

        @Test
        void shouldNotSerializeDocumentWhenHandlerThrowsIoException() throws IOException {
            when(handler.getTitle(PeriodType.MONTHLY)).thenReturn(TITLE);
            when(documentFactory.create(TITLE)).thenReturn(document);
            doThrow(new IOException("generation failed")).when(handler).generate(eq(document), any(PdfReportContext.class));

            assertThrows(IllegalStateException.class,
                    () -> service.execute(PdfReportType.SUM_FINANCES, PeriodType.MONTHLY, USER_ID));

            verify(document, never()).toByteArray();
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            assertThrows(InvalidInputException.class,
                    () -> service.execute(PdfReportType.SUM_FINANCES, null, USER_ID));

            verifyNoInteractions(documentFactory);
        }

        @Test
        void shouldThrowExceptionWhenTypeIsNull() {
            assertThrows(InvalidInputException.class,
                    () -> service.execute(null, PeriodType.MONTHLY, USER_ID));

            verifyNoInteractions(documentFactory);
        }

        @Test
        void shouldThrowExceptionWhenTypeAndPeriodTypeAreNull() {
            assertThrows(InvalidInputException.class, () -> service.execute(null, null, USER_ID));

            verifyNoInteractions(documentFactory);
        }

        @Test
        void shouldThrowExceptionWhenTypeIsNotSupported() {
            assertThrows(InvalidInputException.class,
                    () -> service.execute(PdfReportType.AVERAGE_FINANCES, PeriodType.MONTHLY, USER_ID));

            verifyNoInteractions(documentFactory);
        }

        @Test
        void shouldThrowExceptionWhenDocumentCreationFails() throws IOException {
            when(handler.getTitle(PeriodType.MONTHLY)).thenReturn(TITLE);
            when(documentFactory.create(TITLE)).thenThrow(new IOException("creation failed"));

            assertThrows(IllegalStateException.class,
                    () -> service.execute(PdfReportType.SUM_FINANCES, PeriodType.MONTHLY, USER_ID));
        }

        @Test
        void shouldThrowExceptionWhenHandlerGenerationFails() throws IOException {
            when(handler.getTitle(PeriodType.MONTHLY)).thenReturn(TITLE);
            when(documentFactory.create(TITLE)).thenReturn(document);
            doThrow(new IOException("generation failed")).when(handler).generate(eq(document), any(PdfReportContext.class));

            assertThrows(IllegalStateException.class,
                    () -> service.execute(PdfReportType.SUM_FINANCES, PeriodType.MONTHLY, USER_ID));
        }

        @Test
        void shouldThrowExceptionWhenDocumentSerializationFails() throws IOException {
            when(handler.getTitle(PeriodType.MONTHLY)).thenReturn(TITLE);
            when(documentFactory.create(TITLE)).thenReturn(document);
            when(document.toByteArray()).thenThrow(new IOException("serialization failed"));

            assertThrows(IllegalStateException.class,
                    () -> service.execute(PdfReportType.SUM_FINANCES, PeriodType.MONTHLY, USER_ID));
        }

        @Test
        void shouldThrowExceptionWhenHandlerThrowsRuntimeException() throws IOException {
            when(handler.getTitle(PeriodType.MONTHLY)).thenReturn(TITLE);
            when(documentFactory.create(TITLE)).thenReturn(document);
            doThrow(new IllegalArgumentException("invalid")).when(handler).generate(eq(document), any(PdfReportContext.class));

            assertThrows(IllegalArgumentException.class,
                    () -> service.execute(PdfReportType.SUM_FINANCES, PeriodType.MONTHLY, USER_ID));
        }
    }

    @Nested
    class GetFileName {

        @ParameterizedTest
        @EnumSource(PeriodType.class)
        void shouldReturnFileNameWhenTypeAndPeriodTypeAreValid(PeriodType periodType) {
            when(handler.getFileName(periodType)).thenReturn(FILE_NAME);

            String result = service.getFileName(PdfReportType.SUM_FINANCES, periodType);

            assertEquals(FILE_NAME, result);
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            assertThrows(InvalidInputException.class,
                    () -> service.getFileName(PdfReportType.SUM_FINANCES, null));
        }

        @Test
        void shouldThrowExceptionWhenTypeIsNull() {
            assertThrows(InvalidInputException.class,
                    () -> service.getFileName(null, PeriodType.MONTHLY));
        }

        @Test
        void shouldThrowExceptionWhenTypeIsNotSupported() {
            assertThrows(InvalidInputException.class,
                    () -> service.getFileName(PdfReportType.AVERAGE_FINANCES, PeriodType.MONTHLY));
        }
    }
}