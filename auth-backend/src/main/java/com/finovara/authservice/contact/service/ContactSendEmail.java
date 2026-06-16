package com.finovara.authservice.contact.service;

import com.finovara.authservice.contact.dto.ContactDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactSendEmail {

    private final JavaMailSender mailSender;

    @Value("${mail.recipient.address}")
    private String recipientAddress;

    @Value("${mail.from.address}")
    private String fromAddress;

    @Async
    public void sendContactEmail(ContactDto dto) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientAddress);
        message.setFrom(fromAddress);
        message.setSubject(dto.subject());
        message.setReplyTo(dto.email());
        message.setText("Od: " + dto.username() + " (" + dto.email() + ")\n\n\n" + dto.message());

        log.info("Sending contact email from {}", dto.email());

        try {
            mailSender.send(message);
        } catch (Exception exception) {
            log.error("Failed to send contact email from {}", dto.email(), exception);
        }
    }
}
