package com.finovara.finovarabackend.accountactivity.secure.login.activity.controller;

import com.finovara.finovarabackend.accountactivity.secure.login.activity.dto.LoginActivityDto;
import com.finovara.finovarabackend.accountactivity.secure.login.activity.service.LoginActivityService;
import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.util.confirmationpassword.dto.ConfirmPasswordDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/account-activity/account-security")
@RequiredArgsConstructor
public class LoginActivityController {
    private final LoginActivityService loginActivityService;

    @GetMapping
    public ResponseEntity<List<LoginActivityDto>> getUserLoginActivity() {
        return ResponseEntity.ok(loginActivityService.getLoginActivity(SecurityUtils.getCurrentUserEmail()));
    }

    @PostMapping
    public ResponseEntity<Void> confirmPasswordToUserActivityLogin(@RequestBody ConfirmPasswordDto confirmPasswordDto) {
        loginActivityService.confirmPassword(SecurityUtils.getCurrentUserEmail(), confirmPasswordDto);
        return ResponseEntity.noContent().build();
    }
}
