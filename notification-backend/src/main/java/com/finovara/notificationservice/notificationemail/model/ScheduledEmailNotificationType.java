package com.finovara.notificationservice.notificationemail.model;

public enum ScheduledEmailNotificationType implements EmailNotificationTemplate {

    WEEKLY_FINANCE_DIGEST_REPORT_EMAIL(
            "Finovara - Cotygodniowy Raport finansowy",
            "email/digest/digest-weekly-finance-report-email.html",
            EmailNotificationCategory.SYSTEM),

    WEEKLY_SECURITY_DIGEST_REPORT_EMAIL("Finovara - Cotygodniowy Raport Bezpieczeństwa",
            "email/digest/digest-weekly-security-report-email.html", EmailNotificationCategory.SYSTEM
    );

    private final String subject;
    private final String templatePath;
    private final EmailNotificationCategory category;

    ScheduledEmailNotificationType(String subject, String templatePath, EmailNotificationCategory category) {
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