package com.finovara.notificationservice.notificationemail.util.emailtemplate;

import com.finovara.contracts.exception.serviceunavailable.ServiceUnavailableException;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final JavaMailSender javaMailSender;

    @Value("${mail.recipient.address}")
    private String senderAddress;

    @Async
    public void sendEmail(String recipientEmail, String subject, String templatePath, String username, String templateEmailValue) {
        sendEmail(recipientEmail, subject, templatePath, Map.of(
                "username", username == null ? "" : username,
                "email", templateEmailValue == null ? "" : templateEmailValue
        ));
    }

    @Async
    public void sendEmail(String recipientEmail, String subject, String templatePath, Map<String, String> placeholders) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipientEmail);
            helper.setFrom("Finovara <" + senderAddress + ">");
            helper.setReplyTo(senderAddress);
            helper.setSubject(subject);

            String html = loadTemplate(templatePath, placeholders);
            helper.setText(html, true);

            javaMailSender.send(message);

        } catch (Exception exception) {
            throw new ServiceUnavailableException("Failed to send email", exception);
        }
    }

    private String loadTemplate(String templatePath, Map<String, String> placeholders) {
        try {
            ClassPathResource resource = new ClassPathResource(templatePath);

            try (InputStream inputStream = resource.getInputStream()) {
                String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

                for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                    html = html.replace("{{" + entry.getKey() + "}}", entry.getValue());
                }

                return html;
            }

        } catch (Exception exception) {
            throw new ServiceUnavailableException("Failed to load email template", exception);
        }
    }
}