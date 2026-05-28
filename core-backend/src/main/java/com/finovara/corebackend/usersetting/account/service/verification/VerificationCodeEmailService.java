package com.finovara.corebackend.usersetting.account.service.verification;

import com.finovara.contracts.exception.serviceunavailable.ServiceUnavailableException;
import com.finovara.corebackend.user.model.User;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeEmailService {

    private static final String EMAIL_CHANGE_TEMPLATE_PATH = "email/emailchange/change-email-code.html";
    private static final String PASSWORD_RESET_TEMPLATE_PATH = "email/password/reset-password.html";

    private final JavaMailSender javaMailSender;

    @Value("${mail.recipient.address}")
    private String recipientAddress;

    @Async
    public void sendEmailChangeCode(User user, String email, int code) {
        log.info("Email change code requested: userId={}, email={}", user.getId(), email);
        sendEmail(user, email, code, "Zmiana adresu e-mail", EMAIL_CHANGE_TEMPLATE_PATH);
    }

    @Async
    public void sendPasswordResetCode(User user, String email, int code) {
        log.info("Password reset code requested: userId={}, email={}", user.getId(), email);
        sendEmail(user, email, code, "Przypomnienie Hasła", PASSWORD_RESET_TEMPLATE_PATH);
    }

    private void sendEmail(User user, String email, int code, String subject, String templatePath) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setFrom("Finovara <" + recipientAddress + ">");
            helper.setReplyTo(recipientAddress);
            helper.setSubject(subject);

            String html = loadTemplate(templatePath, user.getUsername(), String.valueOf(code));
            helper.setText(html, true);

            javaMailSender.send(message);
        } catch (Exception exception) {
            throw new ServiceUnavailableException("Email sending failed", exception);
        }
    }

    private String loadTemplate(String templatePath, String username, String code) {
        try {
            ClassPathResource resource = new ClassPathResource(templatePath);

            try (InputStream inputStream = resource.getInputStream()) {
                String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

                return html
                        .replace("{{CODE}}", code)
                        .replace("{{USERNAME}}", username);
            }
        } catch (Exception exception) {
            throw new ServiceUnavailableException("Failed to load template", exception);
        }
    }
}

