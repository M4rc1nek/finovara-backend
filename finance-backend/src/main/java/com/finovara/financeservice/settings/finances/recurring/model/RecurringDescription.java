package com.finovara.financeservice.settings.finances.recurring.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum RecurringDescription {
    EXPENSE("Cykliczne wydatki"),
    REVENUE("Cykliczne przychody");

    private final String label;

    public String label() {
        return label;
    }
}
