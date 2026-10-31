package com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.controller;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.dto.LoginActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.dto.LoginActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.login.activity.service.LoginActivityService;
import com.finovara.activitylogservice.security.SecurityUtils;
import com.finovara.contracts.user.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.util.SortType;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/account-activity/login")
@RequiredArgsConstructor
public class LoginActivityController {
    private final LoginActivityService loginActivityService;

    @GetMapping
    public ResponseEntity<List<LoginActivityDto>> getUserLoginActivity(@RequestParam(defaultValue = "NEWEST") SortType sortType, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(loginActivityService.getLoginActivity(SecurityUtils.getCurrentUserId(), sortType, page));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<LoginActivityDto>> filterLoginActivity(@ModelAttribute LoginActivityFilterDto filter, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(loginActivityService.filterLoginActivity(SecurityUtils.getCurrentUserId(), filter, page));
    }

    @PostMapping("/confirm-password")
    public ResponseEntity<Void> confirmPassword(@RequestBody ConfirmPasswordDto dto) {
        loginActivityService.confirmPassword(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }
}
