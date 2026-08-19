package com.finovara.notificationservice.notificationemail.model;

import lombok.Getter;

@Getter
public enum EmailNotificationType {

    ACCOUNT_DELETED(
            "Finovara - Usunięcie konta",
            "email/account-deleted.html",
            EmailNotificationCategory.SYSTEM),

    EMAIL_CHANGED(
            "Finovara - Zmiana adresu e-mail",
            "email/email-changed.html",
            EmailNotificationCategory.SYSTEM),

    USERNAME_CHANGED(
            "Finovara - Zmiana nazwy użytkownika",
            "email/username-changed.html",
            EmailNotificationCategory.SYSTEM),

    PASSWORD_CHANGED(
            "Finovara - Zmiana hasła",
            "email/password-changed.html",
            EmailNotificationCategory.SYSTEM),

    LARGE_EXPENSE_DETECTED(
            "Finovara - Wykryto duży wydatek",
            "email/large-expense-detected.html",
            EmailNotificationCategory.PREFERENCE_BASED),

    PIGGY_BANK_GOAL_ACHIEVED(
            "Finovara - Cel skarbonki osiągnięty!",
            "email/piggy-bank-goal-achieved.html",
            EmailNotificationCategory.PREFERENCE_BASED),

    DIGEST_REPORT_EMAIL(
            "Finovara - Cotygodniowy Raport finansowy",
            "email/digest/digest-report-email.html",
            EmailNotificationCategory.SYSTEM);

    private final String subject;
    private final String templatePath;
    private final EmailNotificationCategory category;

    EmailNotificationType(String subject, String templatePath, EmailNotificationCategory category) {
        this.subject = subject;
        this.templatePath = templatePath;
        this.category = category;
    }

    public boolean requiresUserOptIn() {
        return category == EmailNotificationCategory.PREFERENCE_BASED;
    }
}