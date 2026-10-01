package com.finovara.contracts.sharedaccount;

public record SharedAccountMemberInfoDto(Long userId, String username, SharedRole role) {
}