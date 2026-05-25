package com.finovara.corebackend.wallet.controller;

import com.finovara.corebackend.wallet.dto.WalletDto;
import com.finovara.corebackend.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.finovara.corebackend.security.SecurityUtils.getCurrentUserId;

@RestController
@RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;

    @GetMapping("/wallet")
    public ResponseEntity<WalletDto> getWallet() {
        return ResponseEntity.ok(walletService.getWalletForUser(getCurrentUserId()));
    }
}
