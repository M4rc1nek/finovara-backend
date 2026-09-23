package com.finovara.authservice.security.oauth2.controller;

import com.finovara.authservice.security.SecurityUtils;
import com.finovara.authservice.security.oauth2.OAuth2LoginCompletionService;
import com.finovara.authservice.security.oauth2.dto.OAuth2LoginResponseDto;
import com.finovara.authservice.security.oauth2.dto.OAuth2PasswordDto;
import com.finovara.authservice.security.oauth2.password.OAuth2PasswordSetter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final OAuth2PasswordSetter oAuth2PasswordSetter;
    private final OAuth2LoginCompletionService oAuth2LoginCompletionService;

    @PostMapping("/set-password")
    public ResponseEntity<Void> createPassword(@RequestBody @Valid OAuth2PasswordDto oAuth2PasswordDto) {
        oAuth2PasswordSetter.createPassword(SecurityUtils.getCurrentUserId(), oAuth2PasswordDto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/complete")
    public OAuth2LoginResponseDto complete(HttpServletRequest request, HttpServletResponse response) {
        return oAuth2LoginCompletionService.complete(request, response);
    }
}
