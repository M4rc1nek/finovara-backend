package com.finovara.finovarabackend.piggybank.controller;

import com.finovara.finovarabackend.piggybank.dto.PiggyBankDto;
import com.finovara.finovarabackend.piggybank.service.PiggyBankManagementService;
import com.finovara.finovarabackend.piggybank.service.PiggyBankTransactionService;
import com.finovara.finovarabackend.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

import static com.finovara.finovarabackend.security.SecurityUtils.getCurrentUserEmail;

@RestController
@RequestMapping("/api/piggy-banks")
@RequiredArgsConstructor
public class PiggyBankManagementController {
    private final PiggyBankManagementService piggyBankManagementService;
    private final PiggyBankTransactionService piggyBankTransactionService;

    @PostMapping
    public ResponseEntity<Long> createPiggyBank(@RequestBody @Valid PiggyBankDto piggyBankDto) {
        return ResponseEntity.ok(piggyBankManagementService.addPiggyBank(piggyBankDto, getCurrentUserEmail()));
    }

    @PatchMapping("/{piggyBankId}")
    public ResponseEntity<Long> editPiggyBank(@RequestBody @Valid PiggyBankDto piggyBankDto, @PathVariable Long piggyBankId) {
        return ResponseEntity.ok(piggyBankManagementService.editPiggyBank(SecurityUtils.getCurrentUserEmail(), piggyBankDto, piggyBankId));
    }

    @GetMapping
    public ResponseEntity<List<PiggyBankDto>> getAllPiggyBanks() {
        return ResponseEntity.ok(piggyBankManagementService.getAllPiggyBanks(getCurrentUserEmail()));
    }

    @DeleteMapping("/{piggyBankId}")
    public ResponseEntity<Void> deletePiggyBank(@PathVariable Long piggyBankId) {
        piggyBankManagementService.deletePiggyBank(getCurrentUserEmail(), piggyBankId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{piggyBankId}/deposit")
    public ResponseEntity<Void> addBalanceToPiggyBank(@PathVariable Long piggyBankId, @RequestParam BigDecimal amount) {
        piggyBankTransactionService.addBalanceToPiggyBank(getCurrentUserEmail(), piggyBankId, amount);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{piggyBankId}/withdraw")
    public ResponseEntity<PiggyBankDto> removeBalanceFromPiggyBank(@PathVariable Long piggyBankId, @RequestParam BigDecimal amount) {
        piggyBankTransactionService.removeBalanceFromPiggyBank(getCurrentUserEmail(), piggyBankId, amount);
        return ResponseEntity.noContent().build();
    }

}
