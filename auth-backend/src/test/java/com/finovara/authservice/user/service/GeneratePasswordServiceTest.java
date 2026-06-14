package com.finovara.authservice.user.service;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class GeneratePasswordServiceTest {

    private GeneratePasswordService generatePasswordService;

    @BeforeEach
    void setUp() {
        generatePasswordService = new GeneratePasswordService();
    }

    @Nested
    class ValidationTests {
        @Test
        void shouldThrowExceptionWhenLengthTooSmall() {
            ReflectionTestUtils.setField(generatePasswordService, "length", 2);

            assertThrows(InvalidInputException.class, () -> generatePasswordService.generatePassword());
        }

        @Test
        void shouldNotThrowWhenLengthIsMinimum() {
            ReflectionTestUtils.setField(generatePasswordService, "length", 3);

            assertDoesNotThrow(() -> generatePasswordService.generatePassword());
        }
    }

    @Nested
    class LengthTests {
        @Test
        void shouldGeneratePasswordWithCorrectLength() {
            ReflectionTestUtils.setField(generatePasswordService, "length", 8);

            String password = generatePasswordService.generatePassword();

            assertEquals(8, password.length());
        }

        @Test
        void shouldGeneratePasswordWithDifferentConfiguredLength() {
            ReflectionTestUtils.setField(generatePasswordService, "length", 12);

            String password = generatePasswordService.generatePassword();

            assertEquals(12, password.length());
        }
    }

    @Nested
    class CharacterRulesTests {
        @Test
        void shouldContainUppercaseDigitAndSpecialCharacter() {
            ReflectionTestUtils.setField(generatePasswordService, "length", 10);

            String password = generatePasswordService.generatePassword();

            assertTrue(password.chars().anyMatch(Character::isUpperCase));
            assertTrue(password.chars().anyMatch(Character::isDigit));
            assertTrue(password.chars().anyMatch(ch -> "!@#$%^&*".indexOf(ch) >= 0));
        }

        @Test
        void shouldContainRequiredCharactersAtMinimumLength() {
            ReflectionTestUtils.setField(generatePasswordService, "length", 3);

            String password = generatePasswordService.generatePassword();

            long upper = password.chars().filter(Character::isUpperCase).count();
            long digits = password.chars().filter(Character::isDigit).count();
            long special = password.chars().filter(ch -> "!@#$%^&*".indexOf(ch) >= 0).count();

            assertTrue(upper >= 1);
            assertTrue(digits >= 1);
            assertTrue(special >= 1);
        }
    }

    @Nested
    class RandomnessTests {
        @Test
        void shouldGenerateDifferentPasswordsEachTime() {
            ReflectionTestUtils.setField(generatePasswordService, "length", 12);

            String first = generatePasswordService.generatePassword();
            String second = generatePasswordService.generatePassword();

            assertNotEquals(first, second);
        }
    }
}