package com.finovara.notificationservice.util.time;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PolishTimeTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    @Test
    void shouldConvertUtcToPolishWinterTime() {
        LocalDateTime utcDateTime = LocalDateTime.of(2026, 11, 5, 10, 15, 30);

        assertEquals(LocalDateTime.of(2026, 11, 5, 11, 15, 30),
                PolishTime.toPolishLocalDateTime(utcDateTime));
        assertEquals("05.11.2026 11:15:30", PolishTime.formatUtcAsPolish(utcDateTime, FORMATTER));
    }

    @Test
    void shouldConvertUtcToPolishSummerTime() {
        LocalDateTime utcDateTime = LocalDateTime.of(2026, 7, 5, 10, 15, 30);

        assertEquals(LocalDateTime.of(2026, 7, 5, 12, 15, 30),
                PolishTime.toPolishLocalDateTime(utcDateTime));
    }
}
