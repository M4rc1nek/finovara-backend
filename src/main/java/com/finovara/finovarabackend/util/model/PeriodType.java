package com.finovara.finovarabackend.util.model;

import java.time.DayOfWeek;
import java.time.LocalDate;

public enum PeriodType {
    DAILY {
        @Override
        public LocalDate getStartDate(LocalDate today) {
            return today;
        }
    },

    WEEKLY {
        @Override
        public LocalDate getStartDate(LocalDate today) {
            return today.with(DayOfWeek.MONDAY);
        }
    },

    MONTHLY {
        @Override
        public LocalDate getStartDate(LocalDate today) {
            return today.withDayOfMonth(1);
        }
    };

    public abstract LocalDate getStartDate(LocalDate today);
}

