package com.finovara.financeservice.piggybank.controller;

import com.finovara.contracts.model.activity.PiggyBankActivityType;
import com.finovara.financeservice.piggybank.dto.PiggyBankDto;
import com.finovara.financeservice.piggybank.service.PiggyBankManagementService;
import com.finovara.financeservice.piggybank.service.PiggyBankTransactionService;
import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.util.transaction.TransactionOrigin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/piggy-banks")
@RequiredArgsConstructor
public class PiggyBankManagementController {
    private final PiggyBankManagementService piggyBankManagementService;
    private final PiggyBankTransactionService piggyBankTransactionService;

    @PostMapping
    public ResponseEntity<Long> createPiggyBank(@RequestBody @Valid PiggyBankDto piggyBankDto) {
        return ResponseEntity.ok(piggyBankManagementService.addPiggyBank(piggyBankDto, SecurityUtils.getCurrentUserId()));
    }

    @PatchMapping("/{piggyBankId}")
    public ResponseEntity<Long> editPiggyBank(@RequestBody @Valid PiggyBankDto piggyBankDto, @PathVariable Long piggyBankId) {
        return ResponseEntity.ok(piggyBankManagementService.editPiggyBank(SecurityUtils.getCurrentUserId(), piggyBankDto, piggyBankId));
    }

    @GetMapping
    public ResponseEntity<List<PiggyBankDto>> getAllPiggyBanks() {
        return ResponseEntity.ok(piggyBankManagementService.getAllPiggyBanks(SecurityUtils.getCurrentUserId()));
    }

    @DeleteMapping("/{piggyBankId}")
    public ResponseEntity<Void> deletePiggyBank(@PathVariable Long piggyBankId, @RequestParam(required = false) String authorizationCode) {
        piggyBankManagementService.deletePiggyBank(SecurityUtils.getCurrentUserId(), piggyBankId, authorizationCode);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{piggyBankId}/deposit")
    public ResponseEntity<Void> addBalanceToPiggyBank(@PathVariable Long piggyBankId, @RequestParam BigDecimal amount, @RequestParam(required = false) String authorizationCode) {
        piggyBankTransactionService.addBalanceToPiggyBank(SecurityUtils.getCurrentUserId(), piggyBankId, amount, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY, authorizationCode, TransactionOrigin.USER_MANUAL);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{piggyBankId}/withdraw")
    public ResponseEntity<PiggyBankDto> removeBalanceFromPiggyBank(@PathVariable Long piggyBankId, @RequestParam BigDecimal amount, @RequestParam(required = false) String authorizationCode) {
        piggyBankTransactionService.removeBalanceFromPiggyBank(SecurityUtils.getCurrentUserId(), piggyBankId, amount, authorizationCode);
        return ResponseEntity.noContent().build();
    }

}
