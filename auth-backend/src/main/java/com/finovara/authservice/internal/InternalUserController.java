package com.finovara.authservice.internal;

import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/")
public class InternalFinanceClientController {

    private final PasswordValidator passwordValidator;
    private final UserManagerService userManagerService;

    @PostMapping("/verify-password")
    public ResponseEntity<Void> verifyPassword(@RequestHeader("X-User-Id") Long userId, @RequestBody ConfirmPasswordDto dto) {
        passwordValidator.validatePassword(userId, dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/username")
    public ResponseEntity<String> getUsername(@RequestHeader("X-User-Id") Long userId){
        return ResponseEntity.ok(userManagerService.getUsernameByIdOrThrow(userId));
    }
}
