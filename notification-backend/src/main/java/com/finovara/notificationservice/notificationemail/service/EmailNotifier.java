package com.finovara.notificationservice.notificationemail.service;

import com.finovara.notificationservice.notificationemail.model.EmailNotificationTemplate;
import com.finovara.notificationservice.notificationemail.util.emailtemplate.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailNotifier {

    private final EmailTemplateService emailTemplateService;

    public void send(EmailNotificationTemplate type, String recipientEmail, Map<String, String> placeholders) {
        emailTemplateService.sendEmail(recipientEmail, type.getSubject(), type.getTemplatePath(), placeholders);
    }
}