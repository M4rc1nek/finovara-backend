package com.finovara.notificationservice.util.time;

import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class PolishTime {

    private static final ZoneId POLAND_ZONE = ZoneId.of("Europe/Warsaw");

    public static LocalDateTime toPolishLocalDateTime(LocalDateTime utcDateTime) {
        return utcDateTime.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(POLAND_ZONE)
                .toLocalDateTime();
    }

    public static String formatUtcAsPolish(LocalDateTime utcDateTime, DateTimeFormatter formatter) {
        return toPolishLocalDateTime(utcDateTime).format(formatter);
    }
}
