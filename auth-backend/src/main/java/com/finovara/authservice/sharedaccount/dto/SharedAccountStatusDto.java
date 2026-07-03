package com.finovara.authservice.sharedaccount.dto;

public record SharedAccountStatusDto(
        boolean hasSharedAccount,
        Long accountId
) {
}
