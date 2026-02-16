package com.finovara.finovarabackend.usersettings.account.controller;

import com.finovara.finovarabackend.usersettings.account.dto.passwordpolicy.ForgotPasswordDto;
import com.finovara.finovarabackend.usersettings.account.dto.passwordpolicy.PasswordRequestDto;
import com.finovara.finovarabackend.usersettings.account.service.passwordpolicy.ForgotPasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forgot-password")
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;

    @PostMapping("/validateEmailExists")
    public ResponseEntity<Void> isEmailExists(@RequestBody @Valid ForgotPasswordDto forgotPasswordDto) {
        forgotPasswordService.validateEmailExists(forgotPasswordDto.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<Void> sendEmail(@RequestBody @Valid ForgotPasswordDto forgotPasswordDto) {
        forgotPasswordService.emailSend(forgotPasswordDto.email(), forgotPasswordDto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/verify-code")
    public ResponseEntity<Void> verifyCode(@RequestBody @Valid ForgotPasswordDto forgotPasswordDto) {
        forgotPasswordService.verifyCode(forgotPasswordDto.email(), forgotPasswordDto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/change-password")
    public ResponseEntity<Void> changePasswordWithCode(@RequestBody @Valid PasswordRequestDto passwordRequestDto) {
        forgotPasswordService.changePasswordWithCode(passwordRequestDto.forgotPasswordDto().email(), passwordRequestDto);
        return ResponseEntity.noContent().build();
    }

}
