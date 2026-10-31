package com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.controller;

import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.dto.AccountChangeActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.dto.AccountChangeActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.secure.accountchange.activity.service.AccountChangeActivityService;
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
@RequestMapping("/api/account-activity/account-changes")
@RequiredArgsConstructor
public class AccountChangeActivityController {

    private final AccountChangeActivityService accountChangeActivityService;

    @GetMapping
    public ResponseEntity<List<AccountChangeActivityDto>> getAccountChangeActivity(@RequestParam(defaultValue = "NEWEST") SortType sortType, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(accountChangeActivityService.getAccountChangeActivity(SecurityUtils.getCurrentUserId(), sortType, page));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<AccountChangeActivityDto>> filterAccountChangeActivity(@ModelAttribute AccountChangeActivityFilterDto filter, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(accountChangeActivityService.filterAccountChangeActivity(SecurityUtils.getCurrentUserId(), filter, page));
    }

    @PostMapping("/confirm-password")
    public ResponseEntity<Void> confirmPassword(@RequestBody ConfirmPasswordDto dto) {
        accountChangeActivityService.confirmPassword(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }

}
