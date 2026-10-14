package com.finovara.reportservice.pdfexport.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfReportDocumentFactoryTest {

    private static final String TITLE = "Tytuł raportu";

    private PdfReportDocumentFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PdfReportDocumentFactory();
    }

    @Nested
    class Create {

        @Test
        void shouldReturnDocumentWhenTitleIsProvided() throws IOException {
            PdfReportDocument document = factory.create(TITLE);

            assertNotNull(document);
            document.close();
        }

        @Test
        void shouldReturnNewInstanceWhenCalledTwice() throws IOException {
            PdfReportDocument first = factory.create(TITLE);
            PdfReportDocument second = factory.create(TITLE);

            assertNotSame(first, second);
            first.close();
            second.close();
        }

        @Test
        void shouldReturnDocumentWithContentWhenTitleIsNull() throws IOException {
            PdfReportDocument document = factory.create(null);

            assertTrue(document.toByteArray().length > 0);
            document.close();
        }
    }
}