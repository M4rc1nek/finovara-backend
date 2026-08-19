package com.finovara.notificationservice.notificationemail.service;

import com.finovara.notificationservice.notificationemail.model.EmailNotificationTemplate;
import com.finovara.notificationservice.notificationemail.util.emailtemplate.EmailTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailNotifierTest {

    private static final String RECIPIENT_EMAIL = "recipient@example.com";
    private static final String SUBJECT = "Subject";
    private static final String TEMPLATE_PATH = "template/path.html";

    @Mock
    private EmailTemplateService emailTemplateService;

    @Mock
    private EmailNotificationTemplate type;

    private EmailNotifier emailNotifier;

    @BeforeEach
    void setUp() {
        emailNotifier = new EmailNotifier(emailTemplateService);
        when(type.getSubject()).thenReturn(SUBJECT);
        when(type.getTemplatePath()).thenReturn(TEMPLATE_PATH);
    }

    @Nested
    class Send {

        @Test
        void shouldCallEmailTemplateServiceWithCorrectArgumentsWhenSendIsInvoked() {
            Map<String, String> placeholders = Map.of("username", "john");

            emailNotifier.send(type, RECIPIENT_EMAIL, placeholders);

            verify(emailTemplateService).sendEmail(RECIPIENT_EMAIL, SUBJECT, TEMPLATE_PATH, placeholders);
        }

        @Test
        void shouldSendEmailWithEmptyPlaceholdersWhenPlaceholdersMapIsEmpty() {
            Map<String, String> placeholders = Map.of();

            emailNotifier.send(type, RECIPIENT_EMAIL, placeholders);

            verify(emailTemplateService).sendEmail(RECIPIENT_EMAIL, SUBJECT, TEMPLATE_PATH, placeholders);
        }

        @Test
        void shouldThrowExceptionWhenEmailTemplateServiceThrows() {
            Map<String, String> placeholders = Map.of("username", "john");
            doThrow(new RuntimeException("template rendering failed"))
                    .when(emailTemplateService).sendEmail(RECIPIENT_EMAIL, SUBJECT, TEMPLATE_PATH, placeholders);

            assertThrows(RuntimeException.class, () -> emailNotifier.send(type, RECIPIENT_EMAIL, placeholders));
        }
    }
}