package com.finovara.financeservice.internal.digest.report.email.service;

import com.finovara.contracts.notification.email.digest.report.PiggyBankSummaryDto;
import com.finovara.financeservice.piggybank.model.PiggyBank;
import com.finovara.financeservice.piggybank.repository.PiggyBankRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PiggyBankDigestServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate FROM = LocalDate.of(2026, 8, 10);
    private static final LocalDate TO = LocalDate.of(2026, 8, 16);

    @Mock
    private PiggyBankRepository piggyBankRepository;

    private PiggyBankDigestService service;

    @BeforeEach
    void setUp() {
        service = new PiggyBankDigestService(piggyBankRepository);
    }

    private PiggyBank piggyBank(BigDecimal amount, BigDecimal goalAmount) {
        PiggyBank piggyBank = mock(PiggyBank.class);
        when(piggyBank.getAmount()).thenReturn(amount);
        when(piggyBank.getGoalAmount()).thenReturn(goalAmount);
        return piggyBank;
    }

    @Nested
    class CalculateSummary {

        @Test
        void shouldReturnZeroValuesWhenPiggyBanksListIsEmpty() {
            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of());
            when(piggyBankRepository.countPiggyBanksByUserIdAndCreatedAtBetween(USER_ID, FROM, TO)).thenReturn(0L);

            PiggyBankSummaryDto result = service.calculateSummary(USER_ID, FROM, TO);

            assertEquals(0L, result.quantityOfPiggyBanks());
            assertEquals(0, BigDecimal.ZERO.compareTo(result.totalDepositedMoney()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.progressPercentage()));
            assertEquals(0, BigDecimal.ZERO.compareTo(result.remainingAmount()));
            assertFalse(result.goalCompleted());
        }

        @Test
        void shouldCalculateTotalDepositedMoneyCorrectly() {
            PiggyBank first = piggyBank(new BigDecimal("100"), new BigDecimal("200"));
            PiggyBank second = piggyBank(new BigDecimal("50"), new BigDecimal("200"));

            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(first, second));
            when(piggyBankRepository.countPiggyBanksByUserIdAndCreatedAtBetween(USER_ID, FROM, TO)).thenReturn(2L);

            PiggyBankSummaryDto result = service.calculateSummary(USER_ID, FROM, TO);

            assertEquals(0, new BigDecimal("150").compareTo(result.totalDepositedMoney()));
        }

        @Test
        void shouldReturnZeroProgressWhenTotalGoalAmountIsZero() {
            PiggyBank piggyBank = piggyBank(new BigDecimal("50"), null);

            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(piggyBank));
            when(piggyBankRepository.countPiggyBanksByUserIdAndCreatedAtBetween(USER_ID, FROM, TO)).thenReturn(1L);

            PiggyBankSummaryDto result = service.calculateSummary(USER_ID, FROM, TO);

            assertEquals(0, BigDecimal.ZERO.compareTo(result.progressPercentage()));
        }

        @Test
        void shouldCalculateProgressPercentageCorrectly() {
            PiggyBank piggyBank = piggyBank(new BigDecimal("50"), new BigDecimal("200"));

            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(piggyBank));
            when(piggyBankRepository.countPiggyBanksByUserIdAndCreatedAtBetween(USER_ID, FROM, TO)).thenReturn(1L);

            PiggyBankSummaryDto result = service.calculateSummary(USER_ID, FROM, TO);

            assertEquals(0, new BigDecimal("25").compareTo(result.progressPercentage()));
        }

        @Test
        void shouldCalculateRemainingAmountCorrectly() {
            PiggyBank piggyBank = piggyBank(new BigDecimal("50"), new BigDecimal("200"));

            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(piggyBank));
            when(piggyBankRepository.countPiggyBanksByUserIdAndCreatedAtBetween(USER_ID, FROM, TO)).thenReturn(1L);

            PiggyBankSummaryDto result = service.calculateSummary(USER_ID, FROM, TO);

            assertEquals(0, new BigDecimal("150").compareTo(result.remainingAmount()));
        }

        @Test
        void shouldReturnZeroRemainingAmountWhenGoalAlreadyExceeded() {
            PiggyBank piggyBank = piggyBank(new BigDecimal("300"), new BigDecimal("200"));

            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(piggyBank));
            when(piggyBankRepository.countPiggyBanksByUserIdAndCreatedAtBetween(USER_ID, FROM, TO)).thenReturn(1L);

            PiggyBankSummaryDto result = service.calculateSummary(USER_ID, FROM, TO);

            assertEquals(0, BigDecimal.ZERO.compareTo(result.remainingAmount()));
        }

        @Test
        void shouldExcludePiggyBanksWithoutGoalFromRemainingAmount() {
            PiggyBank withGoal = piggyBank(new BigDecimal("50"), new BigDecimal("200"));
            PiggyBank withoutGoal = piggyBank(new BigDecimal("30"), null);

            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(withGoal, withoutGoal));
            when(piggyBankRepository.countPiggyBanksByUserIdAndCreatedAtBetween(USER_ID, FROM, TO)).thenReturn(2L);

            PiggyBankSummaryDto result = service.calculateSummary(USER_ID, FROM, TO);

            assertEquals(0, new BigDecimal("150").compareTo(result.remainingAmount()));
        }

        @Test
        void shouldReturnFalseGoalCompletedWhenNoPiggyBanksHaveGoal() {
            PiggyBank withoutGoal = piggyBank(new BigDecimal("30"), null);

            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(withoutGoal));
            when(piggyBankRepository.countPiggyBanksByUserIdAndCreatedAtBetween(USER_ID, FROM, TO)).thenReturn(1L);

            PiggyBankSummaryDto result = service.calculateSummary(USER_ID, FROM, TO);

            assertFalse(result.goalCompleted());
        }

        @Test
        void shouldReturnTrueGoalCompletedWhenAllGoalsCompleted() {
            PiggyBank completed = piggyBank(new BigDecimal("200"), new BigDecimal("200"));

            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(completed));
            when(piggyBankRepository.countPiggyBanksByUserIdAndCreatedAtBetween(USER_ID, FROM, TO)).thenReturn(1L);

            PiggyBankSummaryDto result = service.calculateSummary(USER_ID, FROM, TO);

            assertTrue(result.goalCompleted());
        }

        @Test
        void shouldReturnFalseGoalCompletedWhenAtLeastOneGoalNotCompleted() {
            PiggyBank completed = piggyBank(new BigDecimal("200"), new BigDecimal("200"));
            PiggyBank notCompleted = piggyBank(new BigDecimal("50"), new BigDecimal("200"));

            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of(completed, notCompleted));
            when(piggyBankRepository.countPiggyBanksByUserIdAndCreatedAtBetween(USER_ID, FROM, TO)).thenReturn(2L);

            PiggyBankSummaryDto result = service.calculateSummary(USER_ID, FROM, TO);

            assertFalse(result.goalCompleted());
        }

        @Test
        void shouldReturnQuantityFromRepository() {
            when(piggyBankRepository.findAllByUserId(USER_ID)).thenReturn(List.of());
            when(piggyBankRepository.countPiggyBanksByUserIdAndCreatedAtBetween(USER_ID, FROM, TO)).thenReturn(5L);

            PiggyBankSummaryDto result = service.calculateSummary(USER_ID, FROM, TO);

            assertEquals(5L, result.quantityOfPiggyBanks());
        }
    }
}