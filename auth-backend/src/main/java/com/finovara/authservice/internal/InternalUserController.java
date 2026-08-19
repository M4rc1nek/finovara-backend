package com.finovara.authservice.internal;

import com.finovara.authservice.settings.security.operationauthorization.service.AdditionalAuthorizationService;
import com.finovara.authservice.util.confirmationpassword.service.PasswordValidator;
import com.finovara.authservice.util.user.service.UserManagerService;
import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.authorization.dto.ConfirmAuthorizationCodeDto;
import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.authorization.dto.UserDataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/")
public class InternalUserController {

    private final PasswordValidator passwordValidator;
    private final AdditionalAuthorizationService additionalAuthorizationService;
    private final AdditionalAuthorizationCodeResolver additionalAuthorizationCodeResolver;
    private final UserManagerService userManagerService;

    @PostMapping("/verify-password")
    public ResponseEntity<Void> verifyPassword(@RequestHeader("X-User-Id") Long userId, @RequestBody ConfirmPasswordDto dto) {
        passwordValidator.validatePassword(userId, dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/confirm-authorization-code")
    public ResponseEntity<Void> confirmAuthorizationCode(@RequestHeader("X-User-Id") Long userId, @RequestBody ConfirmAuthorizationCodeDto confirmAuthorizationCodeDto) {
        additionalAuthorizationService.confirmAdditionalAuthorizationCode(userId, additionalAuthorizationCodeResolver.resolve(confirmAuthorizationCodeDto));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/username")
    public ResponseEntity<String> getUsername(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(userManagerService.getUsernameByIdOrThrow(userId));
    }

    @GetMapping("/email")
    public ResponseEntity<String> getUserEmail(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(userManagerService.getUserEmailById(userId));
    }

    @GetMapping("/user-data")
    public ResponseEntity<UserDataResponse> getUserData(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(userManagerService.getUserData(userId));
    }

    @GetMapping("/user-ids")
    public ResponseEntity<List<Long>> getAllUserIds() {
        return ResponseEntity.ok(userManagerService.getAllUserIds());
    }

}
