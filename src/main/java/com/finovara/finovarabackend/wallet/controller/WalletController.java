package com.finovara.finovarabackend.wallet.controller;

import com.finovara.finovarabackend.wallet.dto.WalletDTO;
import com.finovara.finovarabackend.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.finovara.finovarabackend.security.SecurityUtils.getCurrentUserEmail;

@RestController
@RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;

    @GetMapping("/wallet")
    public ResponseEntity<WalletDTO> getWallet() {
        return ResponseEntity.ok(walletService.getWalletForUser(getCurrentUserEmail()));
    }
}

