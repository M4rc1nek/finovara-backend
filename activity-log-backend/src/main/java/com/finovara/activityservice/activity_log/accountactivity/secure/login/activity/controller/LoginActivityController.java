package com.finovara.activityservice.activity_log.accountactivity.secure.login.activity.controller;

import com.finovara.activityservice.activity_log.accountactivity.secure.login.activity.dto.LoginActivityDto;
import com.finovara.activityservice.activity_log.accountactivity.secure.login.activity.service.LoginActivityService;
import com.finovara.activityservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
