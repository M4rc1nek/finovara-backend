package com.finovara.corebackend.contact.service;

import com.finovara.corebackend.contact.dto.ContactDto;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contactService, "recipientAddress", "finovaracenter@gmail.com");
        ReflectionTestUtils.setField(contactService, "fromAddress", "support@finovara.pl");
    }

    @Test
    void shouldSendEmailWithCorrectFields() {
        ContactDto dto = new ContactDto("Jan Kowalski", "Mam pytanie", "Pomoc", "jan@example.com");
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        contactService.sendContactEmail(dto);

        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();

        assertThat(sent.getTo()).containsExactly("finovaracenter@gmail.com");
        assertThat(sent.getFrom()).isEqualTo("support@finovara.pl");
        assertThat(sent.getSubject()).isEqualTo("Pomoc");
        assertThat(sent.getReplyTo()).isEqualTo("jan@example.com");
        assertThat(sent.getText()).contains("Jan Kowalski", "jan@example.com", "Mam pytanie");
    }

    @Test
    void shouldNotThrowWhenMailSenderFails() {
        ContactDto dto = new ContactDto("Jan Kowalski", "Mam pytanie", "Pomoc", "jan@example.com");
        doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> contactService.sendContactEmail(dto)).doesNotThrowAnyException();
    }

    @Test
    void shouldCallMailSenderExactlyOnce() {
        ContactDto dto = new ContactDto("Anna Nowak", "Wiadomość", "Temat", "anna@example.com");

        contactService.sendContactEmail(dto);

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}