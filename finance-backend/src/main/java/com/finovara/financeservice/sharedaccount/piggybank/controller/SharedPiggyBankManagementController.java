package com.finovara.financeservice.sharedaccount.controller.piggybank;

import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.sharedaccount.dto.piggybank.SharedPiggyBankDto;
import com.finovara.financeservice.sharedaccount.service.piggybank.SharedPiggyBankManagementService;
import com.finovara.financeservice.sharedaccount.service.piggybank.SharedPiggyBankTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/shared-accounts/piggy-banks")
@RequiredArgsConstructor
public class SharedPiggyBankManagementController {
    private final SharedPiggyBankManagementService sharedPiggyBankManagementService;
    private final SharedPiggyBankTransactionService sharedPiggyBankTransactionService;

    @PostMapping
    public ResponseEntity<Long> createPiggyBank(@RequestBody @Valid SharedPiggyBankDto sharedPiggyBankDto) {
        return ResponseEntity.ok(sharedPiggyBankManagementService.addPiggyBank(sharedPiggyBankDto, SecurityUtils.getCurrentUserId()));
    }

    @PatchMapping("/{piggyBankId}")
    public ResponseEntity<Long> editPiggyBank(@RequestBody @Valid SharedPiggyBankDto sharedPiggyBankDto, @PathVariable Long piggyBankId) {
        return ResponseEntity.ok(sharedPiggyBankManagementService.editPiggyBank(SecurityUtils.getCurrentUserId(), sharedPiggyBankDto, piggyBankId));
    }

    @GetMapping
    public ResponseEntity<List<SharedPiggyBankDto>> getAllPiggyBanks() {
        return ResponseEntity.ok(sharedPiggyBankManagementService.getAllPiggyBanks(SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/{piggyBankId}")
    public ResponseEntity<Void> deletePiggyBank(@PathVariable Long piggyBankId) {
        sharedPiggyBankManagementService.deletePiggyBank(SecurityUtils.getCurrentUserId(), piggyBankId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{piggyBankId}/deposit")
    public ResponseEntity<BigDecimal> addBalanceToPiggyBank(@PathVariable Long piggyBankId, @RequestParam BigDecimal amount) {
        BigDecimal progress = sharedPiggyBankTransactionService.addBalanceToPiggyBank(SecurityUtils.getCurrentUserId(), piggyBankId, amount);
        return ResponseEntity.ok(progress);
    }

    @PostMapping("/{piggyBankId}/withdraw")
    public ResponseEntity<BigDecimal> removeBalanceFromPiggyBank(@PathVariable Long piggyBankId, @RequestParam BigDecimal amount) {
        BigDecimal progress = sharedPiggyBankTransactionService.removeBalanceFromPiggyBank(SecurityUtils.getCurrentUserId(), piggyBankId, amount);
        return ResponseEntity.ok(progress);
    }
}