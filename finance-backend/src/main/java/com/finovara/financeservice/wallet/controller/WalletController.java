package com.finovara.financeservice.wallet.controller;

import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.wallet.dto.WalletDto;
import com.finovara.financeservice.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;

    @GetMapping("/wallet")
    public ResponseEntity<WalletDto> getWallet() {
        return ResponseEntity.ok(walletService.getWalletForUser(SecurityUtils.getCurrentUserId()));
    }
}
