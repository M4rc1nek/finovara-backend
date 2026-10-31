package com.finovara.activitylogservice.activitylog.mainaccount.core;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DateRange(LocalDateTime from, LocalDateTime to) {

    public static DateRange of(LocalDate date) {
        if (date == null) {
            return new DateRange(null, null);
        }

        return new DateRange(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }
}
