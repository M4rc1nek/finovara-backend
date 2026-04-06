package com.finovara.finovarabackend.util.finances.piggybank;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.util.piggybank.PiggyBankValidator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PiggyBankValidatorTest {

    @Nested
    class ValidateAmount{
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


}