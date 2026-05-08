package com.finovara.finovarabackend.pdfexport.report.service;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.pdfexport.report.document.PdfReportDocument;
import com.finovara.finovarabackend.pdfexport.report.document.PdfReportDocumentFactory;
import com.finovara.finovarabackend.pdfexport.report.model.PdfReportType;
import com.finovara.finovarabackend.util.model.PeriodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfExecutionServiceTest {

    @Mock
    private PdfReportDocumentFactory factory;

    @Mock
    private ReportPdfStrategy strategy;

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
        void shouldReturnPdfBytes() throws Exception {
            when(strategy.getTitle(PeriodType.MONTHLY)).thenReturn("title");
            when(factory.create("title")).thenReturn(document);
            when(document.toByteArray()).thenReturn(new byte[]{1, 2, 3});

            byte[] result = service.execute(PdfReportType.SUM_FINANCES, PeriodType.MONTHLY, 1L);

            assertThat(result).isEqualTo(new byte[]{1, 2, 3});
            verify(strategy).generate(document, 1L, PeriodType.MONTHLY);
            verify(document).close();
        }

        @Test
        void shouldThrowWhenPeriodTypeIsNull() {
            assertThatThrownBy(() -> service.execute(PdfReportType.SUM_FINANCES, null, 1L)).isInstanceOf(InvalidInputException.class);
        }

        @Test
        void shouldThrowWhenTypeIsNull() {
            assertThatThrownBy(() -> service.execute(null, PeriodType.MONTHLY, 1L)).isInstanceOf(InvalidInputException.class);
        }

        @Test
        void shouldThrowWhenStrategyMissing() {
            PdfExecutionService local = new PdfExecutionService(factory, List.of(strategy));

            assertThatThrownBy(() -> local.execute(PdfReportType.AVERAGE_FINANCES, PeriodType.MONTHLY, 1L)).isInstanceOf(InvalidInputException.class);
        }

        @Test
        void shouldWrapIOException() throws Exception {
            when(strategy.getTitle(any())).thenReturn("title");
            when(factory.create(any())).thenReturn(document);
            when(document.toByteArray()).thenThrow(new IOException());

            assertThatThrownBy(() -> service.execute(PdfReportType.SUM_FINANCES, PeriodType.MONTHLY, 1L)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class GetFileName {

        @Test
        void shouldReturnFileName() {
            when(strategy.getFileName(PeriodType.DAILY)).thenReturn("file.pdf");

            String result = service.getFileName(PdfReportType.SUM_FINANCES, PeriodType.DAILY);

            assertThat(result).isEqualTo("file.pdf");
        }

        @Test
        void shouldThrowWhenTypeIsNull() {
            assertThatThrownBy(() -> service.getFileName(null, PeriodType.DAILY)).isInstanceOf(InvalidInputException.class);
        }

        @Test
        void shouldThrowWhenPeriodIsNull() {
            assertThatThrownBy(() -> service.getFileName(PdfReportType.SUM_FINANCES, null)).isInstanceOf(InvalidInputException.class);
        }
    }
}