package com.finovara.notificationservice.notificationemail.util.emailsender;

import com.finovara.notificationservice.notificationemail.model.EmailNotificationType;
import com.finovara.notificationservice.notificationemail.util.emailtemplate.EmailTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotifierTest {

    @Mock
    private EmailTemplateService emailTemplateService;

    @InjectMocks
    private EmailNotifier emailNotifier;

    private Long userId;
    private String username;
    private String email;

    @BeforeEach
    void setUp() {
        userId = 1L;
        username = "john_doe";
        email = "john@example.com";
    }

    @Nested
    class Send {

        @Test
        void shouldSendAccountDeletedEmailWhenTypeIsAccountDeleted() {
            emailNotifier.send(EmailNotificationType.ACCOUNT_DELETED, userId, username, email);

            verify(emailTemplateService).sendEmail(eq(email), anyString(), eq("email/account-deleted.html"), eq(username), eq(email));
        }

        @Test
        void shouldSendEmailChangedEmailWhenTypeIsEmailChanged() {
            emailNotifier.send(EmailNotificationType.EMAIL_CHANGED, userId, username, email);

            verify(emailTemplateService).sendEmail(eq(email), anyString(), eq("email/email-changed.html"), eq(username), eq(email));
        }

        @Test
        void shouldSendUsernameChangedEmailWhenTypeIsUsernameChanged() {
            emailNotifier.send(EmailNotificationType.USERNAME_CHANGED, userId, username, email);

            verify(emailTemplateService).sendEmail(eq(email), anyString(), eq("email/username-changed.html"), eq(username), isNull());
        }

        @Test
        void shouldSendPasswordChangedEmailWhenTypeIsPasswordChanged() {
            emailNotifier.send(EmailNotificationType.PASSWORD_CHANGED, userId, username, email);

            verify(emailTemplateService).sendEmail(eq(email), anyString(), eq("email/password-changed.html"), eq(username), isNull());
        }

        @Test
        void shouldThrowExceptionWhenTypeRequiresExtraDataForLargeExpense() {
            assertThrows(UnsupportedOperationException.class,
                    () -> emailNotifier.send(EmailNotificationType.LARGE_EXPENSE_DETECTED, userId, username, email));

            verifyNoInteractions(emailTemplateService);
        }

        @Test
        void shouldThrowExceptionWhenTypeRequiresExtraDataForGoalAchieved() {
            assertThrows(UnsupportedOperationException.class,
                    () -> emailNotifier.send(EmailNotificationType.PIGGY_BANK_GOAL_ACHIEVED, userId, username, email));

            verifyNoInteractions(emailTemplateService);
        }
    }

    @Nested
    class SendLargeExpenseDetected {

        @Test
        void shouldBuildPlaceholdersAndSendEmailWhenDataIsValid() {
            LocalDateTime occurredAt = LocalDateTime.of(2026, Month.JUNE, 5, 7, 8);
            BigDecimal amount = new BigDecimal("150.00");
            BigDecimal threshold = new BigDecimal("100.00");
            Map<String, String> expectedPlaceholders = Map.of(
                    "username", "Anna",
                    "triggeredByUsername", "Jan",
                    "amount", "150.00",
                    "threshold", "100.00",
                    "occurredAt", "05.06.2026 07:08"
            );

            emailNotifier.sendLargeExpenseDetected("recipient@example.com", "Anna", "Jan", amount, threshold, occurredAt);

            verify(emailTemplateService).sendEmail(
                    eq("recipient@example.com"),
                    eq("Finovara - Wykryto duży wydatek"),
                    eq("email/large-expense-detected.html"),
                    eq(expectedPlaceholders)
            );
        }
    }

    @Nested
    class SendGoalAchieved {

        @Test
        void shouldBuildPlaceholdersAndSendEmailWhenDataIsValid() {
            LocalDateTime occurredAt = LocalDateTime.of(2026, Month.JUNE, 5, 7, 8);
            BigDecimal currentAmount = new BigDecimal("200.00");
            BigDecimal goalAmount = new BigDecimal("300.00");
            Map<String, String> expectedPlaceholders = Map.of(
                    "username", "Anna",
                    "triggeredByUsername", "Jan",
                    "currentAmount", "200.00",
                    "goalAmount", "300.00",
                    "occurredAt", "05.06.2026 07:08"
            );

            emailNotifier.sendGoalAchieved("recipient@example.com", "Anna", "Jan", currentAmount, goalAmount, occurredAt);

            verify(emailTemplateService).sendEmail(
                    eq("recipient@example.com"),
                    eq("Finovara - Cel skarbonki osiągnięty!"),
                    eq("email/piggy-bank-goal-achieved.html"),
                    eq(expectedPlaceholders)
            );
        }
    }
}
