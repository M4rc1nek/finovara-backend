package com.finovara.finovarabackend.usersetting.account.service.passwordpolicy;

import com.finovara.finovarabackend.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordChangeEmailService {

    private final JavaMailSender javaMailSender;

    @Value("${mail.recipient.address}")
    String recipientAddress;

    @Async
    public void sendEmail(User user) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setFrom("Finovara <" + recipientAddress + ">");
        message.setReplyTo(recipientAddress);
        message.setSubject("Finovara - Zmiana hasła");
        message.setText("Witaj " + user.getUsername() + ", \n \n twoje hasło do konta Finovara zostało zmienione. \n \n" +
                " Jeżeli to nie ty, natychmiast skontaktuj się z centrem pomocy pod e-mailem: marcin.parsniak@op.pl \n Pozdrawiamy, \n Finovara");

        javaMailSender.send(message);
    }
}
