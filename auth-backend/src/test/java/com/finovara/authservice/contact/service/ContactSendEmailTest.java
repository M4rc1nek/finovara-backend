package com.finovara.authservice.contact.service;

import com.finovara.authservice.contact.dto.ContactDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactSendEmailTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private ContactSendEmail contactService;

    private ContactDto testDto;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contactService, "recipientAddress", "finovaracenter@gmail.com");
        ReflectionTestUtils.setField(contactService, "fromAddress", "support@finovara.pl");
        testDto = new ContactDto("Jan Kowalski", "Mam pytanie", "Pomoc", "jan@example.com");
    }

    @Nested
    class SendContactEmail {

        @Test
        void shouldSendEmailWithCorrectFieldsWhenDtoIsValid() {
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

            contactService.sendContactEmail(testDto);

            verify(mailSender).send(captor.capture());
            SimpleMailMessage sent = captor.getValue();

            assertThat(sent.getTo()).containsExactly("finovaracenter@gmail.com");
            assertThat(sent.getFrom()).isEqualTo("support@finovara.pl");
            assertThat(sent.getSubject()).isEqualTo("Pomoc");
            assertThat(sent.getReplyTo()).isEqualTo("jan@example.com");
            assertThat(sent.getText()).contains("Jan Kowalski", "jan@example.com", "Mam pytanie");
        }

        @Test
        void shouldNotThrowExceptionWhenMailSenderFails() {
            doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

            assertThatCode(() -> contactService.sendContactEmail(testDto)).doesNotThrowAnyException();
        }

        @Test
        void shouldCallMailSenderExactlyOnceWhenDtoIsValid() {
            contactService.sendContactEmail(testDto);

            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        }

        @Test
        void shouldIncludeContactNameInEmailBodyWhenSendingEmail() {
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

            contactService.sendContactEmail(testDto);

            verify(mailSender).send(captor.capture());
            SimpleMailMessage sent = captor.getValue();

            assertThat(sent.getText()).contains("Jan Kowalski");
        }

        @Test
        void shouldIncludeContactEmailInEmailBodyWhenSendingEmail() {
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

            contactService.sendContactEmail(testDto);

            verify(mailSender).send(captor.capture());
            SimpleMailMessage sent = captor.getValue();

            assertThat(sent.getText()).contains("jan@example.com");
        }

        @Test
        void shouldIncludeContactMessageInEmailBodyWhenSendingEmail() {
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

            contactService.sendContactEmail(testDto);

            verify(mailSender).send(captor.capture());
            SimpleMailMessage sent = captor.getValue();

            assertThat(sent.getText()).contains("Mam pytanie");
        }
    }
}