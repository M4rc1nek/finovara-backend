package com.finovara.finovarabackend.usersetting.account.service.verification.credentialvalidation;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.exception.unprocessablecontent.MissingRequirementException;
import com.finovara.finovarabackend.user.exception.conflict.EmailAlreadyExistsException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.user.repository.UserRepository;
import com.finovara.finovarabackend.usersetting.account.service.verification.CredentialValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidateEmailChangeTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CredentialValidationService credentialValidationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("old@mail.com");
    }

    static Stream<Arguments> invalidEmailsProvider() {
        return Stream.of(Arguments.of(
                null,
                "Email cannot be empty"),
                Arguments.of(null, "Email cannot be empty"));
    }

    @ParameterizedTest
    @MethodSource("invalidEmailsProvider")
    void shouldThrowWhenEmailIsEmpty(String email, String expectedMessage) {

        MissingRequirementException exception = assertThrows(MissingRequirementException.class, ()
                -> credentialValidationService.validateEmailChange(user, email));

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void shouldThrowWhenEmailIsSameAsCurrent() {
        InvalidInputException exception = assertThrows(InvalidInputException.class, () ->
                credentialValidationService.validateEmailChange(user, "OLD@mail.com"));

        assertEquals("New mail cannot be the same", exception.getMessage());
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("new@mail.com")).thenReturn(true);

      EmailAlreadyExistsException exception = assertThrows(com.finovara.finovarabackend.user.exception.conflict.EmailAlreadyExistsException.class, ()
              -> credentialValidationService.validateEmailChange(user, "new@mail.com"));

        assertEquals("Email already in use", exception.getMessage());
    }

    @Test
    void shouldPassWhenEmailIsValid() {
        when(userRepository.existsByEmail("new@mail.com")).thenReturn(false);

        assertDoesNotThrow(() -> credentialValidationService.validateEmailChange(user, "new@mail.com"));
    }
}