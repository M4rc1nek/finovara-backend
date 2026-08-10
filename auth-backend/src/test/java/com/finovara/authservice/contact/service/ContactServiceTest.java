package com.finovara.authservice.contact.service;

import com.finovara.authservice.contact.dto.ContactDto;
import com.finovara.authservice.util.email.EmailDomainValidator;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private EmailDomainValidator emailDomainValidator;

    @Mock
    private ContactSendEmail contactSendEmail;

    @InjectMocks
    private ContactService contactService;

    private ContactDto validDto;

    @BeforeEach
    void setUp() {
        validDto = new ContactDto(
                "John Doe",
                "Hello, I need help with my account",
                "Support request",
                "john@finovara.com"
        );
    }

    @Nested
    class RequestContactEmail {

        @Test
        void shouldValidateEmailAndSendContactEmailWhenDataIsValid() {
            contactService.requestContactEmail(validDto);

            verify(emailDomainValidator).validateDomainHasMxRecord("john@finovara.com");
            verify(contactSendEmail).sendContactEmail(validDto);
        }

        @Test
        void shouldNotSendEmailWhenValidationFails() {
            doThrow(new InvalidInputException("No MX record"))
                    .when(emailDomainValidator)
                    .validateDomainHasMxRecord("john@finovara.com");

            assertThrows(InvalidInputException.class,
                    () -> contactService.requestContactEmail(validDto));

            verify(emailDomainValidator).validateDomainHasMxRecord("john@finovara.com");
            verifyNoInteractions(contactSendEmail);
        }
    }
}