package com.finovara.finovarabackend.wallet.controller;

import com.finovara.finovarabackend.wallet.dto.WalletDTO;
import com.finovara.finovarabackend.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;

    @GetMapping("/wallet")
    public ResponseEntity<WalletDTO> getWallet() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(walletService.getWalletForUser(email));
    }
}

