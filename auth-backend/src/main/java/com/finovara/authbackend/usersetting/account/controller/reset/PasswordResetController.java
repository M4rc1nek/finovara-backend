package com.finovara.authbackend.usersetting.account.controller.reset;

import com.finovara.authbackend.usersetting.account.dto.AttemptsDto;
import com.finovara.authbackend.usersetting.account.dto.passwordpolicy.PasswordResetConfirmDto;
import com.finovara.authbackend.usersetting.account.dto.passwordpolicy.PasswordResetRequestDto;
import com.finovara.authbackend.usersetting.account.service.passwordpolicy.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/request")
    public ResponseEntity<Void> requestPasswordReset(@RequestBody @Valid PasswordResetRequestDto passwordResetRequestDto) {
        passwordResetService.requestPasswordReset(passwordResetRequestDto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/confirm")
    public ResponseEntity<AttemptsDto> confirmPasswordReset(@RequestBody @Valid PasswordResetConfirmDto passwordResetConfirmDto, HttpServletRequest request) {
      return ResponseEntity.ok(passwordResetService.confirmPasswordReset(passwordResetConfirmDto, request));
    }

}
