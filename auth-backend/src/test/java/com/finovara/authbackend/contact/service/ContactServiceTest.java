package com.finovara.authbackend.contact.service;

import com.finovara.authbackend.contact.dto.ContactDto;
import com.finovara.contracts.exception.badrequest.InvalidInputException;
import com.finovara.authbackend.util.email.EmailDomainValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private EmailDomainValidator emailDomainValidator;

    @Mock
    private ContactSendEmail contactSendEmail;

    @InjectMocks
    private ContactService contactService;

    private ContactDto dto;

    @BeforeEach
    void setUp() {
        dto = new ContactDto(
                "John Doe",
                "Hello, I need help with my account",
                "Support request",
                "john@finovara.com"
        );
    }

    @Test
    void shouldValidateEmailAndSendContactEmail() {
        contactService.requestContactEmail(dto);

        verify(emailDomainValidator).validateDomainHasMxRecord("john@finovara.com");
        verify(contactSendEmail).sendContactEmail(dto);
    }

    @Test
    void shouldNotSendEmailWhenValidationFails() {
        doThrow(new InvalidInputException("No MX record"))
                .when(emailDomainValidator)
                .validateDomainHasMxRecord("john@finovara.com");

        try {

            contactService.requestContactEmail(dto);
        } catch (Exception exception) {
        }

        verify(emailDomainValidator).validateDomainHasMxRecord("john@finovara.com");
        verifyNoInteractions(contactSendEmail);
    }
}