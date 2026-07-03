package com.finovara.authservice.user.dto;

public record UserDataDto(

    Long id,

    String username,

    String email,

    String profileImagePath
){}