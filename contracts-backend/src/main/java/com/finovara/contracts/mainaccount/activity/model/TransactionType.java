package com.finovara.contracts.mainaccount.activity.model;

public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    REFUND,
    RESERVATION,
    RESERVATION_RELEASE,
    RESERVATION_ADDED_TO_TRASH,
    RESERVATION_RESTORED_FROM_TRASH,
    RESERVATION_PERMANENTLY_DELETED
}
