package com.finovara.notificationservice.notificationemail.service.settings.action.wallet.lowbalance.controller;

import com.finovara.notificationservice.notificationemail.dto.NotificationEmailDto;
import com.finovara.notificationservice.notificationemail.service.settings.action.usernamechange.service.NotifyUsernameChangeServiceAction;
import com.finovara.notificationservice.notificationemail.service.settings.action.wallet.lowbalance.dto.WalletLowBalanceDto;
import com.finovara.notificationservice.notificationemail.service.settings.action.wallet.lowbalance.service.NotifyWalletLowBalanceServiceAction;
import com.finovara.notificationservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification-settings/notify-wallet-low-balance")
@RequiredArgsConstructor
public class NotifyWalletLowBalanceController {

    private final NotifyWalletLowBalanceServiceAction notifyWalletLowBalanceServiceAction;

    @PatchMapping
    public ResponseEntity<Void> saveNotifyWalletLowBalance(@RequestBody WalletLowBalanceDto dto) {
        notifyWalletLowBalanceServiceAction.saveEmailNotification(SecurityUtils.getCurrentUserId(), dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<WalletLowBalanceDto> getNotifyWalletLowBalance() {
        return ResponseEntity.ok(notifyWalletLowBalanceServiceAction.getEmailNotification(SecurityUtils.getCurrentUserId()));
    }
}
