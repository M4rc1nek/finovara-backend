package com.finovara.financeservice.piggybank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.finovara.financeservice.settings.piggybank.completion.model.GoalCompletionStrategy;
import com.finovara.financeservice.settings.piggybank.model.PiggyBankSettings;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PiggyBankSettingsFactoryTest {

    private PiggyBankSettingsFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PiggyBankSettingsFactory();
    }

    @Nested
    class CreateDefaultPiggyBankSettings {

        @Test
        void shouldReturnSettingsWhenCreatingDefaultSettings() {
            PiggyBankSettings result = factory.createDefaultPiggyBankSettings();

            assertNotNull(result);
        }

        @Test
        void shouldReturnInactiveAutomationWhenCreatingDefaultSettings() {
            PiggyBankSettings result = factory.createDefaultPiggyBankSettings();

            assertFalse(result.isAutomationActive());
        }

        @Test
        void shouldReturnZeroAutomationPercentageWhenCreatingDefaultSettings() {
            PiggyBankSettings result = factory.createDefaultPiggyBankSettings();

            assertEquals(0, BigDecimal.ZERO.compareTo(result.getAutomationPercentage()));
        }

        @Test
        void shouldReturnInactiveRoundUpWhenCreatingDefaultSettings() {
            PiggyBankSettings result = factory.createDefaultPiggyBankSettings();

            assertFalse(result.isRoundUpActive());
        }

        @Test
        void shouldReturnNoneGoalCompletionStrategyWhenCreatingDefaultSettings() {
            PiggyBankSettings result = factory.createDefaultPiggyBankSettings();

            assertEquals(GoalCompletionStrategy.NONE, result.getGoalCompletionStrategy());
        }

        @Test
        void shouldReturnNewInstanceWhenCreatingDefaultSettingsMultipleTimes() {
            PiggyBankSettings first = factory.createDefaultPiggyBankSettings();
            PiggyBankSettings second = factory.createDefaultPiggyBankSettings();

            assertNotSame(first, second);
        }
    }
}