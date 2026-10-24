package com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.controller;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.dto.AccountChangeActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.service.AccountChangeActivityService;
import com.finovara.activitylogservice.security.SecurityUtils;
import com.finovara.contracts.user.authorization.dto.ConfirmPasswordDto;
import com.finovara.contracts.util.SortType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/account-activity/account-changes")
@RequiredArgsConstructor
public class AccountChangeActivityController {

    private final AccountChangeActivityService accountChangeActivityService;

    @GetMapping
    public ResponseEntity<List<AccountChangeActivityDto>> getAccountChangeActivity(@RequestParam(defaultValue = "NEWEST") SortType sortType, @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(accountChangeActivityService.getAccountChangeActivity(SecurityUtils.getCurrentUserId(), sortType, page));
    }

    @PostMapping("/confirm-password")
    public ResponseEntity<Void> confirmPassword(@RequestBody ConfirmPasswordDto dto) {
        accountChangeActivityService.confirmPassword(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }

}
