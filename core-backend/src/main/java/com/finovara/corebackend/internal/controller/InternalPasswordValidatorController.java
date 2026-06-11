package com.finovara.corebackend.internalcontroller;

import com.finovara.contracts.dto.ConfirmPasswordDto;
import com.finovara.corebackend.util.confirmationpassword.service.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/")
public class InternalPasswordValidatorController {

    //Move this controller to Auth-service

    private final PasswordValidator passwordValidator;

    @PostMapping("/verify-password")
    public ResponseEntity<Void> verifyPassword(@RequestHeader("X-User-Id") Long userId, @RequestBody ConfirmPasswordDto dto) {
        passwordValidator.validatePassword(userId, dto);
        return ResponseEntity.noContent().build();
    }
}
