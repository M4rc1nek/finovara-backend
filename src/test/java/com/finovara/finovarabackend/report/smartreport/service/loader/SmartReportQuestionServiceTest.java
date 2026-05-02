package com.finovara.finovarabackend.report.smartreport.service.loader;

import com.finovara.finovarabackend.report.smartreport.model.SmartReportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SmartReportQuestionServiceTest {

    private SmartReportQuestionService smartReportQuestionService;

    @BeforeEach
    void setUp() {
        smartReportQuestionService = new SmartReportQuestionService();
        smartReportQuestionService.init();
    }

    @Nested
    class GetTypeFromQuestion {

        @Test
        void shouldReturnTypeForExactQuestion() {
            String question = "ile wydalem w tym miesiacu";

            SmartReportType result = smartReportQuestionService.getTypeFromQuestion(question);

            assertThat(result).isNotNull();
        }

        @Test
        void shouldIgnoreCaseAndSpecialCharacters() {
            String question = "ILE WYDAŁEM W TYM MIESIĄCU?";

            SmartReportType result = smartReportQuestionService.getTypeFromQuestion(question);

            assertThat(result).isNotNull();
        }

        @Test
        void shouldNormalizePolishCharacters() {
            String question = "ile wydalem w tym miesiacu";

            SmartReportType result = smartReportQuestionService.getTypeFromQuestion(question);

            assertThat(result).isNotNull();
        }

        @Test
        void shouldReturnNullWhenQuestionNotFound() {
            String question = "to jest jakies losowe pytanie";

            SmartReportType result = smartReportQuestionService.getTypeFromQuestion(question);

            assertThat(result).isNull();
        }
    }

    @Nested
    class Normalization {

        @Test
        void shouldNormalizePolishCharactersAndTrim() {
            String input = "  Zażółć GĘŚLĄ jaźń? ";

            SmartReportType result = smartReportQuestionService.getTypeFromQuestion(input);

            assertThat(result).isNull();
        }
    }
}