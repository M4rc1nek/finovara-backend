package com.finovara.financeservice.internal.digest.report.email.mapper;

import com.finovara.contracts.notification.email.digest.report.PiggyBankSummaryDto;
import com.finovara.contracts.notification.email.digest.report.WeeklyDigestReportDto;
import com.finovara.financeservice.internal.digest.report.email.dto.ExpenseSummary;
import com.finovara.financeservice.internal.digest.report.email.dto.RevenueSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WeeklyDigestReportMapperTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate FROM = LocalDate.of(2026, 8, 10);
    private static final LocalDate TO = LocalDate.of(2026, 8, 16);

    private WeeklyDigestReportMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new WeeklyDigestReportMapper();
    }

    private PiggyBankSummaryDto piggyBankSummaryDto() {
        return new PiggyBankSummaryDto(2L, new BigDecimal("100"), new BigDecimal("40"), new BigDecimal("150"), true);
    }

    @Nested
    class ToDto {

        @Test
        void shouldMapAllExpenseFieldsCorrectly() {
            ExpenseSummary expenseSummary = new ExpenseSummary(new BigDecimal("100"), "FOOD", new BigDecimal("60"), "FOOD", LocalDate.of(2026, 8, 11), 3, new BigDecimal("25"));
            RevenueSummary revenueSummary = new RevenueSummary(new BigDecimal("300"), "SALARY", new BigDecimal("300"), "SALARY", LocalDate.of(2026, 8, 12));

            WeeklyDigestReportDto result = mapper.toDto(USER_ID, FROM, TO, expenseSummary, revenueSummary, piggyBankSummaryDto());

            assertEquals(USER_ID, result.userId());
            assertEquals(FROM, result.weekStart());
            assertEquals(TO, result.weekEnd());
            assertEquals(new BigDecimal("100"), result.expensesSum());
            assertEquals("FOOD", result.topExpenseCategory());
            assertEquals(new BigDecimal("60"), result.highestExpenseAmount());
            assertEquals("FOOD", result.highestExpenseCategory());
            assertEquals(LocalDate.of(2026, 8, 11), result.highestExpenseDate());
            assertEquals(3, result.daysWithoutExpense());
            assertEquals(new BigDecimal("25"), result.remainingBudgetPercentage());
        }

        @Test
        void shouldMapAllRevenueFieldsCorrectly() {
            ExpenseSummary expenseSummary = new ExpenseSummary(new BigDecimal("100"), "FOOD", new BigDecimal("60"), "FOOD", LocalDate.of(2026, 8, 11), 3, new BigDecimal("25"));
            RevenueSummary revenueSummary = new RevenueSummary(new BigDecimal("300"), "SALARY", new BigDecimal("300"), "SALARY", LocalDate.of(2026, 8, 12));

            WeeklyDigestReportDto result = mapper.toDto(USER_ID, FROM, TO, expenseSummary, revenueSummary, piggyBankSummaryDto());

            assertEquals(new BigDecimal("300"), result.revenuesSum());
            assertEquals("SALARY", result.topRevenueCategory());
            assertEquals(new BigDecimal("300"), result.highestRevenueAmount());
            assertEquals("SALARY", result.highestRevenueCategory());
            assertEquals(LocalDate.of(2026, 8, 12), result.highestRevenueDate());
        }

        @Test
        void shouldCalculateSavedMoneyAsRevenuesMinusExpenses() {
            ExpenseSummary expenseSummary = new ExpenseSummary(new BigDecimal("100"), "FOOD", BigDecimal.ZERO, null, null, 0, BigDecimal.ZERO);
            RevenueSummary revenueSummary = new RevenueSummary(new BigDecimal("300"), "SALARY", BigDecimal.ZERO, null, null);

            WeeklyDigestReportDto result = mapper.toDto(USER_ID, FROM, TO, expenseSummary, revenueSummary, piggyBankSummaryDto());

            assertEquals(0, new BigDecimal("200").compareTo(result.savedMoney()));
        }

        @Test
        void shouldReturnNegativeSavedMoneyWhenExpensesExceedRevenues() {
            ExpenseSummary expenseSummary = new ExpenseSummary(new BigDecimal("500"), "FOOD", BigDecimal.ZERO, null, null, 0, BigDecimal.ZERO);
            RevenueSummary revenueSummary = new RevenueSummary(new BigDecimal("200"), "SALARY", BigDecimal.ZERO, null, null);

            WeeklyDigestReportDto result = mapper.toDto(USER_ID, FROM, TO, expenseSummary, revenueSummary, piggyBankSummaryDto());

            assertEquals(0, new BigDecimal("-300").compareTo(result.savedMoney()));
        }

        @Test
        void shouldCopyPiggyBankSummaryFieldsCorrectly() {
            ExpenseSummary expenseSummary = new ExpenseSummary(BigDecimal.ZERO, null, BigDecimal.ZERO, null, null, 0, BigDecimal.ZERO);
            RevenueSummary revenueSummary = new RevenueSummary(BigDecimal.ZERO, null, BigDecimal.ZERO, null, null);

            WeeklyDigestReportDto result = mapper.toDto(USER_ID, FROM, TO, expenseSummary, revenueSummary, piggyBankSummaryDto());

            assertEquals(2L, result.piggyBankSummary().quantityOfPiggyBanks());
            assertEquals(new BigDecimal("100"), result.piggyBankSummary().totalDepositedMoney());
            assertEquals(new BigDecimal("40"), result.piggyBankSummary().progressPercentage());
            assertEquals(new BigDecimal("150"), result.piggyBankSummary().remainingAmount());
            assertEquals(true, result.piggyBankSummary().goalCompleted());
        }

        @Test
        void shouldMapNullTopCategoriesAsNull() {
            ExpenseSummary expenseSummary = new ExpenseSummary(BigDecimal.ZERO, null, BigDecimal.ZERO, null, null, 0, BigDecimal.ZERO);
            RevenueSummary revenueSummary = new RevenueSummary(BigDecimal.ZERO, null, BigDecimal.ZERO, null, null);

            WeeklyDigestReportDto result = mapper.toDto(USER_ID, FROM, TO, expenseSummary, revenueSummary, piggyBankSummaryDto());

            assertNull(result.topExpenseCategory());
            assertNull(result.topRevenueCategory());
            assertNull(result.highestExpenseCategory());
            assertNull(result.highestRevenueCategory());
        }

        @Test
        void shouldMapWeekStartAndWeekEndCorrectly() {
            ExpenseSummary expenseSummary = new ExpenseSummary(BigDecimal.ZERO, null, BigDecimal.ZERO, null, null, 0, BigDecimal.ZERO);
            RevenueSummary revenueSummary = new RevenueSummary(BigDecimal.ZERO, null, BigDecimal.ZERO, null, null);

            WeeklyDigestReportDto result = mapper.toDto(USER_ID, FROM, TO, expenseSummary, revenueSummary, piggyBankSummaryDto());

            assertEquals(FROM, result.weekStart());
            assertEquals(TO, result.weekEnd());
        }

        @Test
        void shouldMapUserIdCorrectly() {
            ExpenseSummary expenseSummary = new ExpenseSummary(BigDecimal.ZERO, null, BigDecimal.ZERO, null, null, 0, BigDecimal.ZERO);
            RevenueSummary revenueSummary = new RevenueSummary(BigDecimal.ZERO, null, BigDecimal.ZERO, null, null);

            WeeklyDigestReportDto result = mapper.toDto(USER_ID, FROM, TO, expenseSummary, revenueSummary, piggyBankSummaryDto());

            assertEquals(USER_ID, result.userId());
        }
    }
}