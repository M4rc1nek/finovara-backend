package com.finovara.financeservice.sharedaccount.wallet.controller;

import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.sharedaccount.wallet.dto.SharedWalletDto;
import com.finovara.financeservice.sharedaccount.wallet.service.SharedWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SharedWalletController {
    private final SharedWalletService sharedWalletService;


    @GetMapping("/shared-wallet")
    public ResponseEntity<SharedWalletDto> getSharedWallet(){
        return  ResponseEntity.ok(sharedWalletService.getWallet(SecurityUtils.getCurrentUserId()));
    }

}
