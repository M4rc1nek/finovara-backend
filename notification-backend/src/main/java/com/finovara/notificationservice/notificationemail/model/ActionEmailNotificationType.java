package com.finovara.notificationservice.notificationemail.model;

public enum ActionEmailNotificationType implements EmailNotificationTemplate {

    ACCOUNT_DELETED(
            "Finovara - Usunięcie konta",
            "email/account-deleted.html",
            EmailNotificationCategory.SYSTEM),


    USERNAME_CHANGED(
            "Finovara - Zmiana nazwy użytkownika",
            "email/username-changed.html",
            EmailNotificationCategory.SYSTEM),

    PASSWORD_CHANGED(
            "Finovara - Zmiana hasła",
            "email/password-changed.html",
            EmailNotificationCategory.SYSTEM),

    EMAIL_CHANGED(
            "Finovara - Zmiana adresu e-mail",
            "email/email-changed.html",
            EmailNotificationCategory.SYSTEM),

    WALLET_LOW_BALANCE(
            "Finovara - Alert niskiego salda",
            "email/wallet-low-balance.html",
            EmailNotificationCategory.SYSTEM),

    SHARED_ACCOUNT_LARGE_EXPENSE_DETECTED(
            "Finovara - Wykryto duży wydatek",
            "email/large-expense-detected.html",
            EmailNotificationCategory.PREFERENCE_BASED),

    SHARED_ACCOUNT_PIGGY_BANK_GOAL_ACHIEVED(
            "Finovara - Cel skarbonki osiągnięty!",
            "email/piggy-bank-goal-achieved.html",
            EmailNotificationCategory.PREFERENCE_BASED);

    private final String subject;
    private final String templatePath;
    private final EmailNotificationCategory category;

    ActionEmailNotificationType(String subject, String templatePath, EmailNotificationCategory category) {
        this.subject = subject;
        this.templatePath = templatePath;
        this.category = category;
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public String getTemplatePath() {
        return templatePath;
    }

    @Override
    public EmailNotificationCategory getCategory() {
        return category;
    }
}