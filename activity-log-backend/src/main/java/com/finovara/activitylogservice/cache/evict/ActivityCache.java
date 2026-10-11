package com.finovara.activitylogservice.cache.evict;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivityCache {

    LOGIN("loginActivity"),
    LOGIN_ARCHIVE("loginActivityArchive"),
    ACCOUNT_CHANGE("accountChangeActivity"),
    ACCOUNT_CHANGE_ARCHIVE("accountChangeArchive"),
    EXPENSE("expenseActivity"),
    REVENUE("revenueActivity"),
    LIMIT("limitActivity"),
    PIGGY_BANK("piggyBankActivity"),
    SETTINGS("settingsActivity"),
    RISK_OPERATION("riskOperationActivity"),
    SHARED_ACCOUNT_CHANGE_HISTORY("sharedAccountChangeHistoryActivity"),
    SHARED_ACCOUNT_ACTIVITY_LOG("shared-account:activity");

    private final String cacheName;

    public String patternFor(Long userId) {
        return cacheName + "::" + userId + ":*";
    }
}