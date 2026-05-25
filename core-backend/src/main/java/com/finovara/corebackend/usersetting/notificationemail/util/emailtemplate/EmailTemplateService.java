package com.finovara.corebackend.usersetting.notificationemail.util.emailtemplate;

import com.finovara.corebackend.exception.serviceunavailable.ServiceUnavailableException;
import com.finovara.corebackend.user.model.User;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {
    private final JavaMailSender javaMailSender;

    @Value("${mail.recipient.address}")
    private String senderAddress;

    @Async
    public void sendEmail(User user, String subject, String templatePath, String username, String email) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setFrom("Finovara <" + senderAddress + ">");
            helper.setReplyTo(senderAddress);
            helper.setSubject(subject);

            String html = loadTemplate(templatePath, username, email);
            helper.setText(html, true);

            javaMailSender.send(message);

        } catch (Exception exception) {
            throw new ServiceUnavailableException("Failed to send email", exception);
        }
    }

    private String loadTemplate(String templatePath, String username, String email) {
        try {
            ClassPathResource resource = new ClassPathResource(templatePath);
            try (InputStream inputStream = resource.getInputStream()) {
                String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

                if (username != null) {
                    html = html.replace("{{username}}", username);
                }
                if (email != null) {
                    html = html.replace("{{email}}", email);
                }
                return html;
            }
        } catch (Exception exception) {
            throw new ServiceUnavailableException("Failed to load email template", exception);
        }
    }
}

