package com.finovara.authservice.user.controller;

import com.finovara.authservice.security.jwt.logout.LogoutService;
import com.finovara.authservice.settings.security.twofactor.dto.TwoFactorCodeRequest;
import com.finovara.authservice.settings.security.twofactor.dto.TwoFactorLoginResponseDto;
import com.finovara.authservice.settings.security.twofactor.service.TwoFactorLoginService;
import com.finovara.authservice.user.dto.UserLoginDto;
import com.finovara.authservice.user.dto.UserLoginRequestDto;
import com.finovara.authservice.user.dto.UserRegisterDto;
import com.finovara.authservice.user.service.UserService;
import com.finovara.authservice.util.authorization.generator.SecretGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/api/auth")
@RestController
public class UserController {

    private final UserService userService;
    private final TwoFactorLoginService twoFactorLoginService;
    private final SecretGenerator secretGenerator;
    private final LogoutService logoutService;

    @PostMapping("/register")
    public ResponseEntity<UserRegisterDto> registerUser(@RequestBody @Valid UserRegisterDto userRegisterDto) {
        return ResponseEntity.ok(userService.registerUser(userRegisterDto));
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginDto> loginUser(@RequestBody @Valid UserLoginRequestDto userLoginDto, HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(userService.loginUser(userLoginDto.email(), userLoginDto.password(), userLoginDto.riskVerificationSourceEventId(), request, response));
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<TwoFactorLoginResponseDto> verifyTwoFactor(@RequestBody @Valid TwoFactorCodeRequest verification, HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(twoFactorLoginService.verifyLoginCode(verification.code(), request, response));
    }

    @PostMapping("/cancel-2fa")
    public ResponseEntity<Void> cancelTwoFactor(HttpServletResponse response) {
        twoFactorLoginService.cancelLogin(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/google")
    public ResponseEntity<Void> loginWithGoogle() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/oauth2/authorization/google")
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        logoutService.logout(request, response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/generatePassword")
    public ResponseEntity<String> generatePasswordForUser() {
        return ResponseEntity.ok(secretGenerator.generatePassword());
    }
}
