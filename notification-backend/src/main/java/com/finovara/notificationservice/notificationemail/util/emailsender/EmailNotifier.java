package com.finovara.notificationservice.notificationemail.util.emailsender;

import com.finovara.notificationservice.notificationemail.model.EmailNotificationType;
import com.finovara.notificationservice.notificationemail.util.emailtemplate.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailNotifier {

    private static final String ACCOUNT_DELETED_TEMPLATE = "email/account-deleted.html";
    private static final String PASSWORD_CHANGED_TEMPLATE = "email/password-changed.html";
    private static final String USERNAME_CHANGED_TEMPLATE = "email/username-changed.html";
    private static final String EMAIL_CHANGED_TEMPLATE = "email/email-changed.html";
    private static final String LARGE_EXPENSE_DETECTED_TEMPLATE = "email/large-expense-detected.html";
    private static final String GOAL_ACHIEVED_TEMPLATE = "email/piggy-bank-goal-achieved.html";

    private final EmailTemplateService emailTemplateService;

    @Async
    public void send(EmailNotificationType type, Long userId, String username, String email) {
        switch (type) {
            case ACCOUNT_DELETED ->
                    emailTemplateService.sendEmail(email, "Finovara - Usunięcie konta", ACCOUNT_DELETED_TEMPLATE, username, email);

            case EMAIL_CHANGED ->
                    emailTemplateService.sendEmail(email, "Finovara - Zmiana adresu e-mail", EMAIL_CHANGED_TEMPLATE, username, email);

            case USERNAME_CHANGED ->
                    emailTemplateService.sendEmail(email, "Finovara - Zmiana nazwy użytkownika", USERNAME_CHANGED_TEMPLATE, username, null);

            case PASSWORD_CHANGED ->
                    emailTemplateService.sendEmail(email, "Finovara - Zmiana hasła", PASSWORD_CHANGED_TEMPLATE, username, null);

            case LARGE_EXPENSE_DETECTED ->
                    throw new UnsupportedOperationException("Use sendLargeExpenseDetected(...) instead - this type needs extra template data");

            case PIGGY_BANK_GOAL_ACHIEVED ->
                    throw new UnsupportedOperationException("Use sendGoalAchieved(...) instead - this type needs extra template data");
        }
    }

    @Async
    public void sendLargeExpenseDetected(String recipientEmail, String recipientUsername, String triggeredByUsername, BigDecimal amount, BigDecimal threshold, LocalDateTime occurredAt) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        Map<String, String> placeholders = Map.of(
                "username", recipientUsername,
                "triggeredByUsername", triggeredByUsername,
                "amount", amount.toPlainString(),
                "threshold", threshold.toPlainString(),
                "occurredAt", occurredAt.format(formatter)
        );

        emailTemplateService.sendEmail(recipientEmail, "Finovara - Wykryto duży wydatek", LARGE_EXPENSE_DETECTED_TEMPLATE, placeholders);
    }

    @Async
    public void sendGoalAchieved(String recipientEmail, String recipientUsername, String triggeredByUsername, BigDecimal currentAmount,
                                 BigDecimal goalAmount, LocalDateTime occurredAt) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");


        Map<String, String> placeholders = Map.of(
                "username", recipientUsername,
                "triggeredByUsername", triggeredByUsername,
                "currentAmount", currentAmount.toPlainString(),
                "goalAmount", goalAmount.toPlainString(),
                "occurredAt", occurredAt.format(formatter)
        );

        emailTemplateService.sendEmail(recipientEmail, "Finovara - Cel skarbonki osiągnięty!", GOAL_ACHIEVED_TEMPLATE, placeholders);
    }
}