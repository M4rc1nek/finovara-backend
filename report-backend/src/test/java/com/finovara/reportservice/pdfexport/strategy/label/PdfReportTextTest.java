package com.finovara.reportservice.pdfexport.strategy.label;

import com.finovara.contracts.util.PeriodType;
import com.finovara.contracts.util.model.ExpenseCategory;
import com.finovara.contracts.util.model.RevenueCategory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdfReportTextTest {

    @Nested
    class PeriodLabel {

        @ParameterizedTest
        @CsvSource({
                "DAILY,Dzienny",
                "WEEKLY,Tygodniowy",
                "MONTHLY,Miesięczny"
        })
        void shouldReturnPolishLabelWhenPeriodTypeIsGiven(PeriodType periodType, String expected) {
            assertEquals(expected, PdfReportText.periodLabel(periodType));
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            assertThrows(NullPointerException.class, () -> PdfReportText.periodLabel(null));
        }
    }

    @Nested
    class ExpenseCategoryLabelFromEnum {

        @ParameterizedTest
        @CsvSource({
                "FOOD,Żywność",
                "TRANSPORT,Transport",
                "HOUSING,Mieszkanie",
                "CLOTHING,Ubrania",
                "EDUCATION,Edukacja",
                "HEALTH,Zdrowie",
                "ENTERTAINMENT,Rozrywka",
                "FAMILY,Rodzina",
                "SAVINGS,Oszczędności",
                "VACATIONS,Wakacje",
                "BILLS,Opłaty",
                "OTHERS,Inne"
        })
        void shouldReturnPolishLabelWhenCategoryIsGiven(ExpenseCategory category, String expected) {
            assertEquals(expected, PdfReportText.expenseCategoryLabel(category));
        }

        @Test
        void shouldThrowExceptionWhenCategoryIsNull() {
            assertThrows(NullPointerException.class,
                    () -> PdfReportText.expenseCategoryLabel((ExpenseCategory) null));
        }
    }

    @Nested
    class RevenueCategoryLabelFromEnum {

        @ParameterizedTest
        @CsvSource({
                "SALARY,Pensja",
                "BUSINESS,Działalność",
                "BONUS,Premia",
                "INVESTMENT,Inwestycje",
                "RENT,Wynajem",
                "GIFT,Prezent",
                "REFUND,Zwrot",
                "BENEFIT,Świadczenia",
                "OTHER,Inne"
        })
        void shouldReturnPolishLabelWhenCategoryIsGiven(RevenueCategory category, String expected) {
            assertEquals(expected, PdfReportText.revenueCategoryLabel(category));
        }

        @Test
        void shouldThrowExceptionWhenCategoryIsNull() {
            assertThrows(NullPointerException.class,
                    () -> PdfReportText.revenueCategoryLabel((RevenueCategory) null));
        }
    }

    @Nested
    class ExpenseCategoryLabelFromString {

        @ParameterizedTest
        @CsvSource({
                "FOOD,Żywność",
                "HOUSING,Mieszkanie",
                "OTHERS,Inne"
        })
        void shouldReturnPolishLabelWhenCategoryNameIsGiven(String category, String expected) {
            assertEquals(expected, PdfReportText.expenseCategoryLabel(category));
        }

        @Test
        void shouldReturnOtherLabelWhenCategoryIsNull() {
            assertEquals("Inne", PdfReportText.expenseCategoryLabel((String) null));
        }

        @Test
        void shouldThrowExceptionWhenCategoryNameIsUnknown() {
            assertThrows(IllegalArgumentException.class,
                    () -> PdfReportText.expenseCategoryLabel("UNKNOWN"));
        }

        @Test
        void shouldThrowExceptionWhenCategoryNameHasDifferentCase() {
            assertThrows(IllegalArgumentException.class,
                    () -> PdfReportText.expenseCategoryLabel("food"));
        }

        @Test
        void shouldThrowExceptionWhenCategoryNameIsBlank() {
            assertThrows(IllegalArgumentException.class,
                    () -> PdfReportText.expenseCategoryLabel(" "));
        }
    }

    @Nested
    class RevenueCategoryLabelFromString {

        @ParameterizedTest
        @CsvSource({
                "SALARY,Pensja",
                "BENEFIT,Świadczenia",
                "OTHER,Inne"
        })
        void shouldReturnPolishLabelWhenCategoryNameIsGiven(String category, String expected) {
            assertEquals(expected, PdfReportText.revenueCategoryLabel(category));
        }

        @Test
        void shouldReturnOtherLabelWhenCategoryIsNull() {
            assertEquals("Inne", PdfReportText.revenueCategoryLabel((String) null));
        }

        @Test
        void shouldThrowExceptionWhenCategoryNameIsUnknown() {
            assertThrows(IllegalArgumentException.class,
                    () -> PdfReportText.revenueCategoryLabel("UNKNOWN"));
        }

        @Test
        void shouldThrowExceptionWhenCategoryNameHasDifferentCase() {
            assertThrows(IllegalArgumentException.class,
                    () -> PdfReportText.revenueCategoryLabel("salary"));
        }

        @Test
        void shouldThrowExceptionWhenCategoryNameIsBlank() {
            assertThrows(IllegalArgumentException.class,
                    () -> PdfReportText.revenueCategoryLabel(" "));
        }
    }

    @Nested
    class FileName {

        @ParameterizedTest
        @CsvSource({
                "DAILY,raport-daily.pdf",
                "WEEKLY,raport-weekly.pdf",
                "MONTHLY,raport-monthly.pdf"
        })
        void shouldReturnFileNameWithLowercasePeriodWhenPeriodTypeIsGiven(PeriodType periodType, String expected) {
            assertEquals(expected, PdfReportText.fileName("raport", periodType));
        }

        @Test
        void shouldReturnFileNameWithNullTextWhenPrefixIsNull() {
            assertEquals("null-daily.pdf", PdfReportText.fileName(null, PeriodType.DAILY));
        }

        @Test
        void shouldThrowExceptionWhenPeriodTypeIsNull() {
            assertThrows(NullPointerException.class, () -> PdfReportText.fileName("raport", null));
        }
    }
}