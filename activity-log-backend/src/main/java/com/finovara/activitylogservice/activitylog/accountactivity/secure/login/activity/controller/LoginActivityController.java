package com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.controller;

import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.dto.LoginActivityDto;
import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.service.LoginActivityService;
import com.finovara.activitylogservice.security.SecurityUtils;
import com.finovara.contracts.auth.dto.ConfirmPasswordDto;
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
        return ResponseEntity.ok(loginActivityService.getLoginActivity(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/confirm-password")
    public ResponseEntity<Void> confirmPassword(@RequestBody ConfirmPasswordDto dto) {
        loginActivityService.confirmPassword(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }
}
