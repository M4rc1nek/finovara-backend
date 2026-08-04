package com.finovara.financeservice.settings.piggybank.roundup.dto;

public record RoundUpDto(
        Boolean roundUpActive,
        String authorizationCode
) {
}
