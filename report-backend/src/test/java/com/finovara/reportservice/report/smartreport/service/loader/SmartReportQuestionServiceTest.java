package com.finovara.reportservice.report.smartreport.service.loader;

import com.finovara.reportservice.report.smartreport.model.SmartReportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

        @ParameterizedTest
        @CsvSource({
                "ile wydałem w tym miesiącu, MONTH_SPENDING",
                "ile wydaję średnio dziennie, AVERAGE_DAY_SPENDING",
                "ile procent wydaje, EXPENSE_RATE",
                "ile procent oszczedzam, SAVINGS_RATE"
        })
        void shouldReturnMatchingTypeForKnownQuestion(String question, SmartReportType expectedType) {
            assertThat(smartReportQuestionService.getTypeFromQuestion(question)).isEqualTo(expectedType);
        }

        @Test
        void shouldReturnNullForUnknownQuestion() {
            assertThat(smartReportQuestionService.getTypeFromQuestion("to jest jakieś losowe pytanie")).isNull();
        }

        @ParameterizedTest
        @CsvSource({
                "ile wydałem w tym miesiącu, ILE WYDAŁEM W TYM MIESIĄCU",
                "ile wydałem w tym miesiącu, ile wydalem w tym miesiacu",
                "ile wydałem w tym miesiącu, ile wydałem w tym miesiącu?",
                "ile wydałem w tym miesiącu,   ile wydałem w tym miesiącu  ",
                "ile wydałem w tym miesiącu, ILE WYDALEM W TYM MIESIACU?"
        })
        void shouldNormalizeInputBeforeMatching(String canonicalQuestion, String variantQuestion) {
            SmartReportType canonical = smartReportQuestionService.getTypeFromQuestion(canonicalQuestion);
            SmartReportType variant = smartReportQuestionService.getTypeFromQuestion(variantQuestion);

            assertThat(variant).isNotNull().isEqualTo(canonical);
        }
    }
}
