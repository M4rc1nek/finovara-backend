package com.finovara.notificationservice.notificationemail.util;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.model.transaction.RevenueCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Slf4j
@Component
public class CategoryLabelResolver {

    private static final Map<ExpenseCategory, String> EXPENSE_LABELS = new EnumMap<>(ExpenseCategory.class);
    private static final Map<RevenueCategory, String> REVENUE_LABELS = new EnumMap<>(RevenueCategory.class);

    static {
        EXPENSE_LABELS.put(ExpenseCategory.FOOD, "Żywność");
        EXPENSE_LABELS.put(ExpenseCategory.TRANSPORT, "Transport");
        EXPENSE_LABELS.put(ExpenseCategory.HOUSING, "Mieszkanie");
        EXPENSE_LABELS.put(ExpenseCategory.CLOTHING, "Ubrania");
        EXPENSE_LABELS.put(ExpenseCategory.EDUCATION, "Edukacja");
        EXPENSE_LABELS.put(ExpenseCategory.HEALTH, "Zdrowie");
        EXPENSE_LABELS.put(ExpenseCategory.ENTERTAINMENT, "Rozrywka");
        EXPENSE_LABELS.put(ExpenseCategory.FAMILY, "Rodzina");
        EXPENSE_LABELS.put(ExpenseCategory.SAVINGS, "Oszczędności");
        EXPENSE_LABELS.put(ExpenseCategory.VACATIONS, "Wakacje");
        EXPENSE_LABELS.put(ExpenseCategory.BILLS, "Opłaty");
        EXPENSE_LABELS.put(ExpenseCategory.OTHERS, "Inne");

        REVENUE_LABELS.put(RevenueCategory.SALARY, "Pensja");
        REVENUE_LABELS.put(RevenueCategory.BUSINESS, "Działalność");
        REVENUE_LABELS.put(RevenueCategory.BONUS, "Premia");
        REVENUE_LABELS.put(RevenueCategory.INVESTMENT, "Inwestycje");
        REVENUE_LABELS.put(RevenueCategory.RENT, "Wynajem");
        REVENUE_LABELS.put(RevenueCategory.GIFT, "Prezent");
        REVENUE_LABELS.put(RevenueCategory.REFUND, "Zwrot");
        REVENUE_LABELS.put(RevenueCategory.BENEFIT, "Świadczenia");
        REVENUE_LABELS.put(RevenueCategory.OTHER, "Inne");
    }

    public String resolveExpenseCategory(ExpenseCategory category) {
        if (category == null) {
            return null;
        }
        return EXPENSE_LABELS.getOrDefault(category, category.name());
    }

    public String resolveRevenueCategory(RevenueCategory category) {
        if (category == null) {
            return null;
        }
        return REVENUE_LABELS.getOrDefault(category, category.name());
    }

    public String resolveExpenseCategoryName(String categoryName) {
        if (categoryName == null) {
            return null;
        }
        try {
            return resolveExpenseCategory(ExpenseCategory.valueOf(categoryName));
        } catch (IllegalArgumentException exception) {
            log.warn("Unknown ExpenseCategory value: '{}' - returning without translation", categoryName);
            return categoryName;
        }
    }

    public String resolveRevenueCategoryName(String categoryName) {
        if (categoryName == null) {
            return null;
        }
        try {
            return resolveRevenueCategory(RevenueCategory.valueOf(categoryName));
        } catch (IllegalArgumentException exception) {
            log.warn("Unknown RevenueCategory value: '{}' - returning without translation", categoryName);
            return categoryName;
        }
    }
}