package com.finovara.notificationservice.notificationemail.util.emailtemplate;

import com.finovara.contracts.exception.serviceunavailable.ServiceUnavailableException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailTemplateServiceTest {

    private static final String USERNAME_CHANGED_TEMPLATE = "email/username-changed.html";

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private EmailTemplateService emailTemplateService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(emailTemplateService, "senderAddress", "test@mail.com");

        mimeMessage = new MimeMessage(Session.getInstance(new Properties()));

        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Nested
    class SendEmailWithUsernameAndEmail {

        @Test
        void shouldSendEmailWhenUsernameAndEmailAreProvided() throws Exception {

            emailTemplateService.sendEmail("john@example.com", "Username changed", USERNAME_CHANGED_TEMPLATE, "john_doe", "john@example.com");

            String content = mimeMessage.getContent().toString();

            assertEquals("Username changed", mimeMessage.getSubject());

            assertEquals("john@example.com", mimeMessage.getAllRecipients()[0].toString());

            assertFalse(content.contains("{{"));

            assertFalse(content.contains("}}"));

            verify(javaMailSender).send(mimeMessage);
        }

        @Test
        void shouldReplaceNullValuesWithEmptyStrings() throws Exception {

            emailTemplateService.sendEmail("john@example.com", "Username changed", USERNAME_CHANGED_TEMPLATE, null, null);

            String content = mimeMessage.getContent().toString();

            assertFalse(content.contains("{{"));

            assertFalse(content.contains("}}"));

            verify(javaMailSender).send(mimeMessage);
        }
    }

    @Nested
    class SendEmailWithPlaceholderMap {

        @Test
        void shouldRenderTemplateWhenPlaceholdersAreProvided() throws Exception {

            Map<String, String> placeholders = Map.of("username", "Anna", "oldUsername", "oldName", "newUsername", "newName", "email", "anna@example.com");

            emailTemplateService.sendEmail("anna@example.com", "Username changed", USERNAME_CHANGED_TEMPLATE, placeholders);

            String content = mimeMessage.getContent().toString();

            assertFalse(content.contains("{{"));

            assertFalse(content.contains("}}"));

            verify(javaMailSender).send(mimeMessage);
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void shouldThrowExceptionWhenTemplateDoesNotExist() {

            assertThrows(ServiceUnavailableException.class, () -> emailTemplateService.sendEmail("john@example.com", "Test", "email/not-existing.html", "john", "john@example.com"));

            verify(javaMailSender).createMimeMessage();

            verify(javaMailSender, never()).send(mimeMessage);
        }
    }
}