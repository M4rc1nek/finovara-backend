package com.finovara.notificationservice.notificationemail.util.emailtemplate;

import com.finovara.contracts.exception.serviceunavailable.ServiceUnavailableException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailTemplateServiceTest {

    private static final String SENDER_ADDRESS = "no-reply@finovara.com";
    private static final String RECIPIENT_EMAIL = "recipient@example.com";
    private static final String SUBJECT = "Test subject";
    private static final String TEMPLATE_PATH = "com/finovara/notificationservice/notificationemail/util/emailtemplate/EmailTemplateService.class";

    private org.springframework.mail.javamail.JavaMailSender javaMailSender;
    private EmailTemplateService service;
    private MimeMessage mimeMessage;

    @Nested
    class SendEmail {

        @BeforeEach
        void setUp() {
            javaMailSender = mock(org.springframework.mail.javamail.JavaMailSender.class);
            mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
            service = new EmailTemplateService(javaMailSender);

            ReflectionTestUtils.setField(service, "senderAddress", SENDER_ADDRESS);

            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        }

        @Test
        void shouldSendEmailWhenTemplateExists() {
            service.sendEmail(RECIPIENT_EMAIL, SUBJECT, TEMPLATE_PATH, Collections.emptyMap());

            verify(javaMailSender).createMimeMessage();
            verify(javaMailSender).send(mimeMessage);
        }

        @Test
        void shouldSendEmailWhenPlaceholdersAreProvided() {
            Map<String, String> placeholders = Map.of("name", "John", "code", "123456");

            service.sendEmail(RECIPIENT_EMAIL, SUBJECT, TEMPLATE_PATH, placeholders);

            verify(javaMailSender).send(mimeMessage);
        }

        @Test
        void shouldSendEmailWhenPlaceholdersAreEmpty() {
            service.sendEmail(RECIPIENT_EMAIL, SUBJECT, TEMPLATE_PATH, Collections.emptyMap());

            verify(javaMailSender).send(mimeMessage);
        }

        @Test
        void shouldThrowExceptionWhenMimeMessageCreationFails() {
            when(javaMailSender.createMimeMessage()).thenThrow(new RuntimeException());

            assertThrows(ServiceUnavailableException.class, () -> service.sendEmail(RECIPIENT_EMAIL, SUBJECT, TEMPLATE_PATH, Collections.emptyMap()));

            verify(javaMailSender).createMimeMessage();
            verify(javaMailSender, never()).send(mimeMessage);
        }

        @Test
        void shouldThrowExceptionWhenTemplateDoesNotExist() {
            assertThrows(ServiceUnavailableException.class, () -> service.sendEmail(RECIPIENT_EMAIL, SUBJECT, "templates/not-existing.html", Collections.emptyMap()));

            verify(javaMailSender).createMimeMessage();
            verify(javaMailSender, never()).send(mimeMessage);
        }

        @Test
        void shouldThrowExceptionWhenTemplatePathIsNull() {
            assertThrows(ServiceUnavailableException.class, () -> service.sendEmail(RECIPIENT_EMAIL, SUBJECT, null, Collections.emptyMap()));

            verify(javaMailSender).createMimeMessage();
            verify(javaMailSender, never()).send(mimeMessage);
        }

        @Test
        void shouldThrowExceptionWhenPlaceholdersAreNull() {
            assertThrows(ServiceUnavailableException.class, () -> service.sendEmail(RECIPIENT_EMAIL, SUBJECT, TEMPLATE_PATH, null));

            verify(javaMailSender).createMimeMessage();
            verify(javaMailSender, never()).send(mimeMessage);
        }

        @Test
        void shouldThrowExceptionWhenSendFails() {
            doThrow(new RuntimeException()).when(javaMailSender).send(mimeMessage);

            assertThrows(ServiceUnavailableException.class, () -> service.sendEmail(RECIPIENT_EMAIL, SUBJECT, TEMPLATE_PATH, Collections.emptyMap()));

            verify(javaMailSender).send(mimeMessage);
        }

        @Test
        void shouldSetRecipientWhenEmailIsSent() {
            service.sendEmail(RECIPIENT_EMAIL, SUBJECT, TEMPLATE_PATH, Collections.emptyMap());

            jakarta.mail.Address[] recipients = assertDoesNotThrow(() -> mimeMessage.getRecipients(jakarta.mail.Message.RecipientType.TO));

            assertNotNull(recipients);
            assertEquals(1, recipients.length);
            assertEquals(RECIPIENT_EMAIL, recipients[0].toString());
        }

        @Test
        void shouldSetSubjectWhenEmailIsSent() {
            service.sendEmail(RECIPIENT_EMAIL, SUBJECT, TEMPLATE_PATH, Collections.emptyMap());

            assertEquals(SUBJECT, assertDoesNotThrow(() -> mimeMessage.getSubject()));
        }

        @Test
        void shouldSetSenderWhenEmailIsSent() {
            service.sendEmail(RECIPIENT_EMAIL, SUBJECT, TEMPLATE_PATH, Collections.emptyMap());

            jakarta.mail.Address[] from = assertDoesNotThrow(() -> mimeMessage.getFrom());

            assertNotNull(from);
            assertEquals(1, from.length);
            assertEquals("Finovara <" + SENDER_ADDRESS + ">", from[0].toString());
        }

        @Test
        void shouldSetReplyToWhenEmailIsSent() {
            service.sendEmail(RECIPIENT_EMAIL, SUBJECT, TEMPLATE_PATH, Collections.emptyMap());

            jakarta.mail.Address[] replyTo = assertDoesNotThrow(() -> mimeMessage.getReplyTo());

            assertNotNull(replyTo);
            assertEquals(1, replyTo.length);
            assertEquals(SENDER_ADDRESS, replyTo[0].toString());
        }

        @Test
        void shouldThrowExceptionWhenRecipientEmailIsNull() {
            assertThrows(ServiceUnavailableException.class, () -> service.sendEmail(null, SUBJECT, TEMPLATE_PATH, Collections.emptyMap()));

            verify(javaMailSender).createMimeMessage();
            verify(javaMailSender, never()).send(mimeMessage);
        }

        @Test
        void shouldThrowExceptionWhenRecipientEmailIsEmpty() {
            assertThrows(ServiceUnavailableException.class, () -> service.sendEmail("", SUBJECT, TEMPLATE_PATH, Collections.emptyMap()));

            verify(javaMailSender).createMimeMessage();
            verify(javaMailSender, never()).send(mimeMessage);
        }
    }
}