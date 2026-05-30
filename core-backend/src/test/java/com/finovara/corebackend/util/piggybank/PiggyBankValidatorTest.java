package com.finovara.corebackend.util.piggybank;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.piggybank.dto.PiggyBankDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PiggyBankValidatorTest {

    @Nested
    class ValidateAmount {
        @Test
        void shouldNotThrowExceptionWhenAmountIsPositive() {
            assertDoesNotThrow(() -> PiggyBankValidator.validateAmount(BigDecimal.valueOf(100)));
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNegative() {
            assertThrows(InvalidInputException.class, () -> PiggyBankValidator.validateAmount(BigDecimal.valueOf(-100)));
        }

        @Test
        void shouldThrowExceptionWhenAmountIsNull() {
            assertThrows(InvalidInputException.class, () -> PiggyBankValidator.validateAmount(null));

        }

        @Test
        void shouldThrowExceptionWhenAmountIsZero() {
            assertThrows(InvalidInputException.class, () -> PiggyBankValidator.validateAmount(BigDecimal.ZERO));
        }
    }

    @Nested
    class ValidateGoalAmount {

        private PiggyBankDto createDto(BigDecimal goalAmount) {
            return new PiggyBankDto(1L, 1L, "Savings", BigDecimal.valueOf(100), LocalDate.now(),
                    null, goalAmount, 0.0, false
            );
        }

        @Test
        void shouldNotThrowExceptionWhenGoalAmountIsNull() {
            PiggyBankDto dto = createDto(null);

            assertDoesNotThrow(() -> PiggyBankValidator.validateGoalAmount(dto));
        }

        @Test
        void shouldNotThrowExceptionWhenGoalAmountIsPositive() {
            PiggyBankDto dto = createDto(BigDecimal.valueOf(100));

            assertDoesNotThrow(() -> PiggyBankValidator.validateGoalAmount(dto));
        }

        @Test
        void shouldThrowExceptionWhenGoalAmountIsZero() {
            PiggyBankDto dto = createDto(BigDecimal.ZERO);

            assertThrows(InvalidInputException.class, () -> PiggyBankValidator.validateGoalAmount(dto));
        }

        @Test
        void shouldThrowExceptionWhenGoalAmountIsNegative() {
            PiggyBankDto dto = createDto(BigDecimal.valueOf(-100));

            assertThrows(InvalidInputException.class, () -> PiggyBankValidator.validateGoalAmount(dto));
        }
    }

    @Nested
    class ValidateSufficientFunds {

        @Test
        void shouldNotThrowExceptionWhenSourceAmountIsGreaterThanAmount() {
            assertDoesNotThrow(() -> PiggyBankValidator.validateSufficientFunds(BigDecimal.valueOf(200), BigDecimal.valueOf(100)));
        }

        @Test
        void shouldNotThrowExceptionWhenSourceAmountEqualsAmount() {
            assertDoesNotThrow(() -> PiggyBankValidator.validateSufficientFunds(BigDecimal.valueOf(100), BigDecimal.valueOf(100)));
        }

        @Test
        void shouldThrowExceptionWhenSourceAmountIsLessThanAmount() {
            assertThrows(InvalidInputException.class, () -> PiggyBankValidator.validateSufficientFunds(BigDecimal.valueOf(50), BigDecimal.valueOf(100)));
        }

        @Test
        void shouldThrowExceptionWhenSourceAmountIsNull() {
            assertThrows(InvalidInputException.class, () -> PiggyBankValidator.validateSufficientFunds(null, BigDecimal.valueOf(100)));
        }
    }
}

