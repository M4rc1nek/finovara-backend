package com.finovara.finovarabackend.accountactivity.login.activities.controller;

import com.finovara.finovarabackend.accountactivity.login.activities.dto.UserActivityLoginDto;
import com.finovara.finovarabackend.accountactivity.login.activities.service.UserActivityLoginService;
import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/account-activity/account-security")
@RequiredArgsConstructor
public class UserActivityLoginController {
    private final UserActivityLoginService userActivityLoginService;

    @GetMapping
    public ResponseEntity<List<UserActivityLoginDto>> getUserActivityLogin() {
        return ResponseEntity.ok(userActivityLoginService.getUserActivityLogin(SecurityUtils.getCurrentUserEmail()));
    }

    @PostMapping
    public ResponseEntity<Void> confirmPasswordToUserActivityLogin(@RequestBody ConfirmPasswordDto confirmPasswordDto) {
        userActivityLoginService.confirmPasswordToUserActivityLogin(SecurityUtils.getCurrentUserEmail(), confirmPasswordDto);
        return ResponseEntity.noContent().build();
    }
}
