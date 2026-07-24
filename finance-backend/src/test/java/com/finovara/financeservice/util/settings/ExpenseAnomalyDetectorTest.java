package com.finovara.financeservice.util.settings;

import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.financeservice.exception.conflict.ConfirmationRequiredException;
import com.finovara.financeservice.feignclient.AuthBackendClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ExpenseAnomalyDetectorTest {

    private ExpenseAnomalyDetector expenseAnomalyDetector;

    @Mock
    private AuthBackendClient authBackendClient;

    @Nested
    class CalculateAnomalyThreshold {

        @BeforeEach
        void setUp() {
            expenseAnomalyDetector = new ExpenseAnomalyDetector();
        }

        @Test
        void shouldCalculateThresholdWhenMultipleAmountsProvided() {
            List<BigDecimal> amounts = List.of(
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(200),
                    BigDecimal.valueOf(300)
            );

            BigDecimal result = expenseAnomalyDetector.calculateAnomalyThreshold(amounts, BigDecimal.valueOf(2));

            assertEquals(BigDecimal.valueOf(400.00).setScale(2), result);
        }

        @Test
        void shouldCalculateThresholdWhenSingleAmountProvided() {
            List<BigDecimal> amounts = List.of(BigDecimal.valueOf(150));

            BigDecimal result = expenseAnomalyDetector.calculateAnomalyThreshold(amounts, BigDecimal.valueOf(3));

            assertEquals(BigDecimal.valueOf(450.00).setScale(2), result);
        }

        @Test
        void shouldReturnZeroWhenAmountsContainOnlyZero() {
            List<BigDecimal> amounts = List.of(BigDecimal.ZERO);

            BigDecimal result = expenseAnomalyDetector.calculateAnomalyThreshold(amounts, BigDecimal.valueOf(5));

            assertEquals(BigDecimal.ZERO.setScale(2), result);
        }

        @Test
        void shouldRoundAverageWhenDivisionProducesDecimalValue() {
            List<BigDecimal> amounts = List.of(
                    BigDecimal.ONE,
                    BigDecimal.valueOf(2)
            );

            BigDecimal result = expenseAnomalyDetector.calculateAnomalyThreshold(amounts, BigDecimal.valueOf(2));

            assertEquals(BigDecimal.valueOf(3.00).setScale(2), result);
        }

        @Test
        void shouldReturnZeroWhenMultiplierIsZero() {
            List<BigDecimal> amounts = List.of(
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(200)
            );

            BigDecimal result = expenseAnomalyDetector.calculateAnomalyThreshold(amounts, BigDecimal.ZERO);

            assertEquals(BigDecimal.ZERO.setScale(2), result);
        }

        @Test
        void shouldThrowExceptionWhenAmountsListIsEmpty() {
            List<BigDecimal> amounts = List.of();

            assertThrows(ArithmeticException.class,
                    () -> expenseAnomalyDetector.calculateAnomalyThreshold(amounts, BigDecimal.valueOf(2)));
        }

        @Test
        void shouldThrowExceptionWhenAmountsListIsNull() {
            assertThrows(NullPointerException.class,
                    () -> expenseAnomalyDetector.calculateAnomalyThreshold(null, BigDecimal.valueOf(2)));
        }

        @Test
        void shouldThrowExceptionWhenMultiplierIsNull() {
            List<BigDecimal> amounts = List.of(
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(200)
            );

            assertThrows(NullPointerException.class,
                    () -> expenseAnomalyDetector.calculateAnomalyThreshold(amounts, null));
        }

    }

    @Nested
    class RequirePasswordConfirmation {

        private Long userId;

        @BeforeEach
        void setUp() {
            expenseAnomalyDetector = new ExpenseAnomalyDetector();
            userId = 1L;
        }

        @Test
        void shouldVerifyPasswordWhenConfirmationDataIsValid() {
            ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto("password");

            expenseAnomalyDetector.requirePasswordConfirmation(userId, confirmPasswordDto, authBackendClient);

            verify(authBackendClient).verifyPassword(userId, confirmPasswordDto);
        }

        @Test
        void shouldThrowExceptionWhenConfirmationDtoIsNull() {
            assertThrows(ConfirmationRequiredException.class,
                    () -> expenseAnomalyDetector.requirePasswordConfirmation(userId, null, authBackendClient));

            verifyNoInteractions(authBackendClient);
        }

        @Test
        void shouldThrowExceptionWhenPasswordIsNull() {
            ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto(null);

            assertThrows(ConfirmationRequiredException.class,
                    () -> expenseAnomalyDetector.requirePasswordConfirmation(userId, confirmPasswordDto, authBackendClient));

            verifyNoInteractions(authBackendClient);
        }

        @Test
        void shouldVerifyPasswordWhenPasswordIsEmpty() {
            ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto("");

            expenseAnomalyDetector.requirePasswordConfirmation(userId, confirmPasswordDto, authBackendClient);

            verify(authBackendClient).verifyPassword(userId, confirmPasswordDto);
        }

        @Test
        void shouldPassNullUserIdToClientWhenUserIdIsNull() {
            ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto("password");

            expenseAnomalyDetector.requirePasswordConfirmation(null, confirmPasswordDto, authBackendClient);

            verify(authBackendClient).verifyPassword(null, confirmPasswordDto);
        }

        @Test
        void shouldThrowExceptionWhenAuthBackendClientIsNull() {
            ConfirmPasswordDto confirmPasswordDto = new ConfirmPasswordDto("password");

            assertThrows(NullPointerException.class,
                    () -> expenseAnomalyDetector.requirePasswordConfirmation(userId, confirmPasswordDto, null));
        }
    }
}