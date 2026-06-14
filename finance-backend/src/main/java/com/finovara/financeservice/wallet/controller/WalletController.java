package com.finovara.authbackend.wallet.controller;

import com.finovara.authbackend.wallet.dto.WalletDto;
import com.finovara.authbackend.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.finovara.authbackend.security.SecurityUtils.getCurrentUserId;

@RestController
@RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;

    @GetMapping("/wallet")
    public ResponseEntity<WalletDto> getWallet() {
        return ResponseEntity.ok(walletService.getWalletForUser(getCurrentUserId()));
    }
}
