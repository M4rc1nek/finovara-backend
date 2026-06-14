package com.finovara.authservice.settings.account.service.passwordpolicy;

import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/")
public class InternalPasswordValidatorController {

    private final PasswordValidator passwordValidator;

    @PostMapping("/verify-password")
    public ResponseEntity<Void> verifyPassword(@RequestHeader("X-User-Id") Long userId, @RequestBody ConfirmPasswordDto dto) {
        passwordValidator.validatePassword(userId, dto);
        return ResponseEntity.noContent().build();
    }
}
