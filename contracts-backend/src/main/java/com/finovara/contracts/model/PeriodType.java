package com.finovara.contracts.model;

import java.time.DayOfWeek;
import java.time.LocalDate;

public enum PeriodType {
    DAILY {
        @Override
        public LocalDate getStartDate(LocalDate today) {
            return today;
        }

        @Override
        public LocalDate addPeriod(LocalDate date) {
            return date.plusDays(1);
        }
    },

    WEEKLY {
        @Override
        public LocalDate getStartDate(LocalDate today) {
            return today.with(DayOfWeek.MONDAY);
        }

        @Override
        public LocalDate addPeriod(LocalDate date) {
            return date.plusWeeks(1);
        }
    },

    MONTHLY {
        @Override
        public LocalDate getStartDate(LocalDate today) {
            return today.withDayOfMonth(1);
        }

        @Override
        public LocalDate addPeriod(LocalDate date) {
            return date.plusMonths(1);
        }
    };

    public abstract LocalDate getStartDate(LocalDate today);

    public abstract LocalDate addPeriod(LocalDate date);
}

