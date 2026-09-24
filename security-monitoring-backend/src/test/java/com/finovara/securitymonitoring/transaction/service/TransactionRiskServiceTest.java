package com.finovara.securitymonitoring.transaction.service;

import com.finovara.contracts.model.transaction.ExpenseCategory;
import com.finovara.contracts.securitymonitoring.model.RiskTriggerType;
import com.finovara.securitymonitoring.riskengine.dto.RiskContext;
import com.finovara.contracts.securitymonitoring.model.RiskRule;
import com.finovara.contracts.securitymonitoring.dto.TriggeredRule;
import com.finovara.securitymonitoring.transaction.config.TransactionRiskProperties;
import com.finovara.securitymonitoring.transaction.model.TransactionProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionRiskServiceTest {

    private static final Long USER_ID = 1L;

    private static final BigDecimal EXPENSE_HIGH_AMOUNT = BigDecimal.valueOf(1000);
    private static final BigDecimal FIRST_TRANSACTION_AMOUNT = BigDecimal.valueOf(500);
    private static final BigDecimal REVENUE_HIGH_AMOUNT = BigDecimal.valueOf(2000);
    private static final BigDecimal REVENUE_LOW_AMOUNT = BigDecimal.valueOf(50);
    private static final BigDecimal PIGGY_BANK_HIGH_AMOUNT = BigDecimal.valueOf(1500);
    private static final BigDecimal PIGGY_BANK_DIFF_AMOUNT = BigDecimal.valueOf(200);

    private static final int EXPENSE_HIGH_POINTS = 20;
    private static final int EXPENSE_RECORD_POINTS = 15;
    private static final int FIRST_TRANSACTION_POINTS = 25;
    private static final int EXPENSE_CATEGORY_POINTS = 10;
    private static final int REVENUE_HIGH_POINTS = 20;
    private static final int REVENUE_LOW_POINTS = 15;
    private static final int PIGGY_BANK_HIGH_POINTS = 20;
    private static final int PIGGY_BANK_DIFF_POINTS = 10;

    private TransactionRiskProperties properties;
    private TransactionRiskService transactionRiskService;

    @BeforeEach
    void setUp() {
        properties = new TransactionRiskProperties();
        properties.setExpenseHighAmount(EXPENSE_HIGH_AMOUNT);
        properties.setFirstTransactionAmount(FIRST_TRANSACTION_AMOUNT);
        properties.setRevenueHighAmount(REVENUE_HIGH_AMOUNT);
        properties.setRevenueLowAmount(REVENUE_LOW_AMOUNT);
        properties.setPiggyBankHighAmount(PIGGY_BANK_HIGH_AMOUNT);
        properties.setPiggyBankDiffAmount(PIGGY_BANK_DIFF_AMOUNT);
        properties.setExpenseHighPoints(EXPENSE_HIGH_POINTS);
        properties.setExpenseRecordPoints(EXPENSE_RECORD_POINTS);
        properties.setFirstTransactionPoints(FIRST_TRANSACTION_POINTS);
        properties.setExpenseCategoryPoints(EXPENSE_CATEGORY_POINTS);
        properties.setRevenueHighPoints(REVENUE_HIGH_POINTS);
        properties.setRevenueLowPoints(REVENUE_LOW_POINTS);
        properties.setPiggyBankHighPoints(PIGGY_BANK_HIGH_POINTS);
        properties.setPiggyBankDiffPoints(PIGGY_BANK_DIFF_POINTS);
        transactionRiskService = new TransactionRiskService(properties);
    }

    private RiskContext context(RiskTriggerType triggerType, BigDecimal amount, String category, TransactionProfile profile) {
        return new RiskContext(USER_ID, triggerType, amount, category, null, null, null, profile, null, null);
    }

    private TransactionProfile.TransactionProfileBuilder defaultProfile() {
        return TransactionProfile.builder().expenseCount(1L).revenueCount(1L);
    }

    @Nested
    class Evaluate {

        @Test
        void shouldReturnEmptyListWhenTransactionProfileIsNull() {
            RiskContext riskContext = context(RiskTriggerType.EXPENSE, BigDecimal.TEN, null, null);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldReturnEmptyListWhenTriggerTypeIsNotHandled() {
            TransactionProfile profile = defaultProfile().build();
            RiskContext riskContext = context(RiskTriggerType.LOGIN, BigDecimal.TEN, null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class ExpenseRules {

        @Test
        void shouldReturnEmptyListWhenNoConditionIsMet() {
            TransactionProfile profile = defaultProfile()
                    .largestExpenseAmount(BigDecimal.valueOf(2000))
                    .build();
            RiskContext riskContext = context(RiskTriggerType.EXPENSE, BigDecimal.valueOf(100), null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldTriggerExpenseHighRuleWhenAmountExceedsThreshold() {
            TransactionProfile profile = defaultProfile().build();
            RiskContext riskContext = context(RiskTriggerType.EXPENSE, EXPENSE_HIGH_AMOUNT.add(BigDecimal.ONE), null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertEquals(1, result.size());
            assertEquals(new TriggeredRule(RiskRule.EXPENSE_HIGH, EXPENSE_HIGH_POINTS), result.get(0));
        }

        @Test
        void shouldNotTriggerExpenseHighRuleWhenAmountEqualsThreshold() {
            TransactionProfile profile = defaultProfile().build();
            RiskContext riskContext = context(RiskTriggerType.EXPENSE, EXPENSE_HIGH_AMOUNT, null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldTriggerExpenseRecordRuleWhenAmountExceedsLargestExpense() {
            TransactionProfile profile = defaultProfile()
                    .largestExpenseAmount(BigDecimal.valueOf(50))
                    .build();
            RiskContext riskContext = context(RiskTriggerType.EXPENSE, BigDecimal.valueOf(100), null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertEquals(1, result.size());
            assertEquals(new TriggeredRule(RiskRule.EXPENSE_RECORD, EXPENSE_RECORD_POINTS), result.get(0));
        }

        @Test
        void shouldNotTriggerExpenseRecordRuleWhenLargestExpenseAmountIsNull() {
            TransactionProfile profile = defaultProfile()
                    .largestExpenseAmount(null)
                    .build();
            RiskContext riskContext = context(RiskTriggerType.EXPENSE, BigDecimal.valueOf(100), null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldTriggerFirstTransactionHighRuleWhenExpenseCountIsZeroAndAmountExceedsThreshold() {
            TransactionProfile profile = defaultProfile()
                    .expenseCount(0L)
                    .build();
            RiskContext riskContext = context(RiskTriggerType.EXPENSE, FIRST_TRANSACTION_AMOUNT.add(BigDecimal.ONE), null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertEquals(1, result.size());
            assertEquals(new TriggeredRule(RiskRule.FIRST_TRANSACTION_HIGH, FIRST_TRANSACTION_POINTS), result.get(0));
        }

        @Test
        void shouldNotTriggerFirstTransactionHighRuleWhenExpenseCountIsNotZero() {
            TransactionProfile profile = defaultProfile()
                    .expenseCount(3L)
                    .build();
            RiskContext riskContext = context(RiskTriggerType.EXPENSE, FIRST_TRANSACTION_AMOUNT.add(BigDecimal.ONE), null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldNotEvaluateAmountRulesWhenAmountIsNull() {
            TransactionProfile profile = defaultProfile()
                    .expenseCount(0L)
                    .largestExpenseAmount(BigDecimal.ONE)
                    .build();
            RiskContext riskContext = context(RiskTriggerType.EXPENSE, null, null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldTriggerExpenseNewCategoryRuleWhenCategoryDiffersFromLastExpenseCategory() {
            TransactionProfile profile = defaultProfile()
                    .lastExpenseCategory(ExpenseCategory.FOOD)
                    .build();
            RiskContext riskContext = context(RiskTriggerType.EXPENSE, BigDecimal.TEN, ExpenseCategory.TRANSPORT.name(), profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertEquals(1, result.size());
            assertEquals(new TriggeredRule(RiskRule.EXPENSE_NEW_CATEGORY, EXPENSE_CATEGORY_POINTS), result.get(0));
        }

        @Test
        void shouldNotTriggerExpenseNewCategoryRuleWhenCategoryMatchesLastExpenseCategory() {
            TransactionProfile profile = defaultProfile()
                    .lastExpenseCategory(ExpenseCategory.FOOD)
                    .build();
            RiskContext riskContext = context(RiskTriggerType.EXPENSE, BigDecimal.TEN, ExpenseCategory.FOOD.name(), profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldNotTriggerExpenseNewCategoryRuleWhenCategoryIsNull() {
            TransactionProfile profile = defaultProfile()
                    .lastExpenseCategory(ExpenseCategory.FOOD)
                    .build();
            RiskContext riskContext = context(RiskTriggerType.EXPENSE, BigDecimal.TEN, null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldNotTriggerExpenseNewCategoryRuleWhenLastExpenseCategoryIsNull() {
            TransactionProfile profile = defaultProfile()
                    .lastExpenseCategory(null)
                    .build();
            RiskContext riskContext = context(RiskTriggerType.EXPENSE, BigDecimal.TEN, ExpenseCategory.FOOD.name(), profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldEvaluateCategoryRuleIndependentlyWhenAmountIsNull() {
            TransactionProfile profile = defaultProfile()
                    .lastExpenseCategory(ExpenseCategory.FOOD)
                    .build();
            RiskContext riskContext = context(RiskTriggerType.EXPENSE, null, ExpenseCategory.TRANSPORT.name(), profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertEquals(1, result.size());
            assertEquals(new TriggeredRule(RiskRule.EXPENSE_NEW_CATEGORY, EXPENSE_CATEGORY_POINTS), result.get(0));
        }

        @Test
        void shouldTriggerAllExpenseRulesWhenAllConditionsMet() {
            TransactionProfile profile = defaultProfile()
                    .expenseCount(0L)
                    .largestExpenseAmount(BigDecimal.valueOf(100))
                    .lastExpenseCategory(ExpenseCategory.FOOD)
                    .build();
            RiskContext riskContext = context(
                    RiskTriggerType.EXPENSE,
                    EXPENSE_HIGH_AMOUNT.add(BigDecimal.valueOf(500)),
                    ExpenseCategory.TRANSPORT.name(),
                    profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertEquals(4, result.size());
            assertTrue(result.contains(new TriggeredRule(RiskRule.EXPENSE_HIGH, EXPENSE_HIGH_POINTS)));
            assertTrue(result.contains(new TriggeredRule(RiskRule.EXPENSE_RECORD, EXPENSE_RECORD_POINTS)));
            assertTrue(result.contains(new TriggeredRule(RiskRule.FIRST_TRANSACTION_HIGH, FIRST_TRANSACTION_POINTS)));
            assertTrue(result.contains(new TriggeredRule(RiskRule.EXPENSE_NEW_CATEGORY, EXPENSE_CATEGORY_POINTS)));
        }
    }

    @Nested
    class RevenueRules {

        @Test
        void shouldReturnEmptyListWhenAmountIsNull() {
            TransactionProfile profile = defaultProfile().build();
            RiskContext riskContext = context(RiskTriggerType.REVENUE, null, null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldTriggerRevenueHighRuleWhenAmountExceedsThreshold() {
            TransactionProfile profile = defaultProfile().build();
            RiskContext riskContext = context(RiskTriggerType.REVENUE, REVENUE_HIGH_AMOUNT.add(BigDecimal.ONE), null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertEquals(1, result.size());
            assertEquals(new TriggeredRule(RiskRule.REVENUE_HIGH, REVENUE_HIGH_POINTS), result.get(0));
        }

        @Test
        void shouldTriggerRevenueLowRuleWhenAmountIsPositiveAndBelowThreshold() {
            TransactionProfile profile = defaultProfile().build();
            RiskContext riskContext = context(RiskTriggerType.REVENUE, BigDecimal.valueOf(10), null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertEquals(1, result.size());
            assertEquals(new TriggeredRule(RiskRule.REVENUE_LOW, REVENUE_LOW_POINTS), result.get(0));
        }

        @Test
        void shouldNotTriggerRevenueLowRuleWhenAmountIsZero() {
            TransactionProfile profile = defaultProfile().build();
            RiskContext riskContext = context(RiskTriggerType.REVENUE, BigDecimal.ZERO, null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldNotTriggerRevenueLowRuleWhenAmountIsNegative() {
            TransactionProfile profile = defaultProfile().build();
            RiskContext riskContext = context(RiskTriggerType.REVENUE, BigDecimal.valueOf(-10), null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldTriggerFirstTransactionHighRuleWhenRevenueCountIsZeroAndAmountExceedsThreshold() {
            TransactionProfile profile = defaultProfile()
                    .revenueCount(0L)
                    .build();
            RiskContext riskContext = context(RiskTriggerType.REVENUE, FIRST_TRANSACTION_AMOUNT.add(BigDecimal.ONE), null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertEquals(1, result.size());
            assertEquals(new TriggeredRule(RiskRule.FIRST_TRANSACTION_HIGH, FIRST_TRANSACTION_POINTS), result.get(0));
        }

        @Test
        void shouldNotTriggerFirstTransactionHighRuleWhenRevenueCountIsNotZero() {
            TransactionProfile profile = defaultProfile()
                    .revenueCount(5L)
                    .build();
            RiskContext riskContext = context(RiskTriggerType.REVENUE, FIRST_TRANSACTION_AMOUNT.add(BigDecimal.ONE), null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldTriggerAllRevenueRulesWhenAllConditionsMet() {
            TransactionProfile profile = defaultProfile()
                    .revenueCount(0L)
                    .build();
            RiskContext riskContext = context(RiskTriggerType.REVENUE, REVENUE_HIGH_AMOUNT.add(BigDecimal.valueOf(600)), null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertEquals(2, result.size());
            assertTrue(result.contains(new TriggeredRule(RiskRule.REVENUE_HIGH, REVENUE_HIGH_POINTS)));
            assertTrue(result.contains(new TriggeredRule(RiskRule.FIRST_TRANSACTION_HIGH, FIRST_TRANSACTION_POINTS)));
        }
    }

    @Nested
    class PiggyBankRules {

        @Test
        void shouldReturnEmptyListWhenAmountIsNull() {
            TransactionProfile profile = defaultProfile().build();
            RiskContext riskContext = context(RiskTriggerType.PIGGY_BANK, null, null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldTriggerPiggyBankHighRuleWhenAmountExceedsThreshold() {
            TransactionProfile profile = defaultProfile().build();
            RiskContext riskContext = context(RiskTriggerType.PIGGY_BANK, PIGGY_BANK_HIGH_AMOUNT.add(BigDecimal.ONE), null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertEquals(1, result.size());
            assertEquals(new TriggeredRule(RiskRule.PIGGY_BANK_HIGH, PIGGY_BANK_HIGH_POINTS), result.get(0));
        }

        @Test
        void shouldTriggerPiggyBankDifferentRuleWhenDiffExceedsThreshold() {
            TransactionProfile profile = defaultProfile()
                    .lastPiggyBankDepositAmount(BigDecimal.valueOf(100))
                    .build();
            RiskContext riskContext = context(RiskTriggerType.PIGGY_BANK, BigDecimal.valueOf(400), null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertEquals(1, result.size());
            assertEquals(new TriggeredRule(RiskRule.PIGGY_BANK_DIFFERENT, PIGGY_BANK_DIFF_POINTS), result.get(0));
        }

        @Test
        void shouldNotTriggerPiggyBankDifferentRuleWhenLastDepositIsNull() {
            TransactionProfile profile = defaultProfile()
                    .lastPiggyBankDepositAmount(null)
                    .build();
            RiskContext riskContext = context(RiskTriggerType.PIGGY_BANK, BigDecimal.valueOf(400), null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldNotTriggerPiggyBankDifferentRuleWhenLastDepositIsZero() {
            TransactionProfile profile = defaultProfile()
                    .lastPiggyBankDepositAmount(BigDecimal.ZERO)
                    .build();
            RiskContext riskContext = context(RiskTriggerType.PIGGY_BANK, BigDecimal.valueOf(400), null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldNotTriggerPiggyBankDifferentRuleWhenDiffIsWithinThreshold() {
            TransactionProfile profile = defaultProfile()
                    .lastPiggyBankDepositAmount(BigDecimal.valueOf(100))
                    .build();
            RiskContext riskContext = context(RiskTriggerType.PIGGY_BANK, BigDecimal.valueOf(150), null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldTriggerPiggyBankDifferentRuleWhenAmountIsLowerThanLastDepositByMoreThanThreshold() {
            TransactionProfile profile = defaultProfile()
                    .lastPiggyBankDepositAmount(BigDecimal.valueOf(500))
                    .build();
            RiskContext riskContext = context(RiskTriggerType.PIGGY_BANK, BigDecimal.valueOf(100), null, profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertEquals(1, result.size());
            assertEquals(new TriggeredRule(RiskRule.PIGGY_BANK_DIFFERENT, PIGGY_BANK_DIFF_POINTS), result.get(0));
        }

        @Test
        void shouldTriggerBothPiggyBankRulesWhenAllConditionsMet() {
            TransactionProfile profile = defaultProfile()
                    .lastPiggyBankDepositAmount(BigDecimal.valueOf(100))
                    .build();
            RiskContext riskContext = context(
                    RiskTriggerType.PIGGY_BANK,
                    PIGGY_BANK_HIGH_AMOUNT.add(BigDecimal.valueOf(500)),
                    null,
                    profile);

            List<TriggeredRule> result = transactionRiskService.evaluate(riskContext);

            assertEquals(2, result.size());
            assertTrue(result.contains(new TriggeredRule(RiskRule.PIGGY_BANK_HIGH, PIGGY_BANK_HIGH_POINTS)));
            assertTrue(result.contains(new TriggeredRule(RiskRule.PIGGY_BANK_DIFFERENT, PIGGY_BANK_DIFF_POINTS)));
        }
    }
}