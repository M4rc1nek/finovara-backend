package com.finovara.authservice.security.oauth2.controller;

import com.finovara.authservice.security.SecurityUtils;
import com.finovara.authservice.security.oauth2.dto.OAuth2PasswordDto;
import com.finovara.authservice.security.oauth2.password.OAuth2PasswordSetter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/set-password")
@RequiredArgsConstructor
public class OAuth2PasswordController {

    private final OAuth2PasswordSetter oAuth2PasswordSetter;

    @PostMapping
    public ResponseEntity<Void> createPassword(@RequestBody @Valid OAuth2PasswordDto oAuth2PasswordDto) {
        oAuth2PasswordSetter.createPassword(SecurityUtils.getCurrentUserId(), oAuth2PasswordDto);
        return ResponseEntity.noContent().build();
    }
}
