package com.finovara.authservice.sharedaccount.dto;

public record SharedAccountDetailsDto(
        Long remainingUserId,
        Long ownerId,
        Long memberId
) {
}
