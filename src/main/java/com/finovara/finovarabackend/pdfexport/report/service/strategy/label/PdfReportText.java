package com.finovara.finovarabackend.pdfexport.report.service.strategy.label;

import com.finovara.finovarabackend.expense.model.ExpenseCategory;
import com.finovara.finovarabackend.revenue.model.RevenueCategory;
import com.finovara.finovarabackend.util.model.PeriodType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PdfReportText {

    public static String periodLabel(PeriodType periodType) {
        return switch (periodType) {
            case DAILY -> "Dzienny";
            case WEEKLY -> "Tygodniowy";
            case MONTHLY -> "Miesięczny";
        };
    }

    public static String expenseCategoryLabel(ExpenseCategory category) {
        return switch (category) {
            case FOOD -> "Żywność";
            case TRANSPORT -> "Transport";
            case HOUSING -> "Mieszkanie";
            case CLOTHING -> "Ubrania";
            case EDUCATION -> "Edukacja";
            case HEALTH -> "Zdrowie";
            case ENTERTAINMENT -> "Rozrywka";
            case FAMILY -> "Rodzina";
            case SAVINGS -> "Oszczędności";
            case VACATIONS -> "Wakacje";
            case BILLS -> "Opłaty";
            case OTHERS -> "Inne";
        };
    }

    public static String revenueCategoryLabel(RevenueCategory category) {
        return switch (category) {
            case SALARY -> "Pensja";
            case BUSINESS -> "Działalność";
            case BONUS -> "Premia";
            case INVESTMENT -> "Inwestycje";
            case RENT -> "Wynajem";
            case GIFT -> "Prezent";
            case REFUND -> "Zwrot";
            case BENEFIT -> "Świadczenia";
            case OTHER -> "Inne";
        };
    }

    public static String fileName(String prefix, PeriodType periodType) {
        return prefix + "-" + periodType.name().toLowerCase() + ".pdf";
    }
}
