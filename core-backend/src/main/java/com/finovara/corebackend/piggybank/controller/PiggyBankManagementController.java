package com.finovara.corebackend.piggybank.controller;

import com.finovara.activityservice.contracts.model.activity.PiggyBankActivityType;
import com.finovara.corebackend.piggybank.dto.PiggyBankDto;
import com.finovara.corebackend.piggybank.service.PiggyBankManagementService;
import com.finovara.corebackend.piggybank.service.PiggyBankTransactionService;
import com.finovara.corebackend.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

import static com.finovara.corebackend.security.SecurityUtils.getCurrentUserId;

@RestController
@RequestMapping("/api/piggy-banks")
@RequiredArgsConstructor
public class PiggyBankManagementController {
    private final PiggyBankManagementService piggyBankManagementService;
    private final PiggyBankTransactionService piggyBankTransactionService;

    @PostMapping
    public ResponseEntity<Long> createPiggyBank(@RequestBody @Valid PiggyBankDto piggyBankDto) {
        return ResponseEntity.ok(piggyBankManagementService.addPiggyBank(piggyBankDto, getCurrentUserId()));
    }

    @PatchMapping("/{piggyBankId}")
    public ResponseEntity<Long> editPiggyBank(@RequestBody @Valid PiggyBankDto piggyBankDto, @PathVariable Long piggyBankId) {
        return ResponseEntity.ok(piggyBankManagementService.editPiggyBank(SecurityUtils.getCurrentUserId(), piggyBankDto, piggyBankId));
    }

    @GetMapping
    public ResponseEntity<List<PiggyBankDto>> getAllPiggyBanks() {
        return ResponseEntity.ok(piggyBankManagementService.getAllPiggyBanks(getCurrentUserId()));
    }

    @DeleteMapping("/{piggyBankId}")
    public ResponseEntity<Void> deletePiggyBank(@PathVariable Long piggyBankId) {
        piggyBankManagementService.deletePiggyBank(getCurrentUserId(), piggyBankId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{piggyBankId}/deposit")
    public ResponseEntity<Void> addBalanceToPiggyBank(@PathVariable Long piggyBankId, @RequestParam BigDecimal amount) {
        piggyBankTransactionService.addBalanceToPiggyBank(getCurrentUserId(), piggyBankId, amount, PiggyBankActivityType.AMOUNT_ADDED_TO_PIGGY_BANK_DIRECTLY);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{piggyBankId}/withdraw")
    public ResponseEntity<PiggyBankDto> removeBalanceFromPiggyBank(@PathVariable Long piggyBankId, @RequestParam BigDecimal amount) {
        piggyBankTransactionService.removeBalanceFromPiggyBank(getCurrentUserId(), piggyBankId, amount);
        return ResponseEntity.noContent().build();
    }

}
