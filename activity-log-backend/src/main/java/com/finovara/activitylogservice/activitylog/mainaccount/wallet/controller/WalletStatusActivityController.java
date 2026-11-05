package com.finovara.activitylogservice.activitylog.mainaccount.wallet.controller;

import com.finovara.activitylogservice.activitylog.mainaccount.wallet.dto.WalletStatusActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.wallet.dto.WalletStatusActivityFilterDto;
import com.finovara.activitylogservice.activitylog.mainaccount.wallet.service.WalletStatusActivityService;
import com.finovara.activitylogservice.security.SecurityUtils;
import com.finovara.contracts.util.SortType;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/account-activity/wallet")
@RequiredArgsConstructor
public class WalletStatusActivityController {

    private final WalletStatusActivityService walletStatusActivityService;

    @GetMapping
    public ResponseEntity<List<WalletStatusActivityDto>> getWalletActivity(@RequestParam(defaultValue = "NEWEST") SortType sort, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(walletStatusActivityService.getWalletActivity(SecurityUtils.getCurrentUserId(), sort, page));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<WalletStatusActivityDto>> filterWalletActivity(@ModelAttribute WalletStatusActivityFilterDto filter, @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(walletStatusActivityService.filterWalletActivity(SecurityUtils.getCurrentUserId(), filter, page));
    }
}
