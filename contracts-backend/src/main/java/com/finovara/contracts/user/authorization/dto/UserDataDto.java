package com.finovara.contracts.user.authorization.dto;

public record UserDataDto(
    Long id,
    String username,
    String email,
    String profileImagePath
){}