package com.finovara.finovarabackend.piggybank.controller;

import com.finovara.finovarabackend.piggybank.dto.PiggyBankDTO;
import com.finovara.finovarabackend.piggybank.service.PiggyBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/piggy-banks")
@RequiredArgsConstructor
public class PiggyBankController {
    private final PiggyBankService piggyBankService;

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping
    public ResponseEntity<PiggyBankDTO> createPiggyBank(@RequestBody @Valid PiggyBankDTO piggyBankDTO) {
        return ResponseEntity.ok(piggyBankService.addPiggyBank(piggyBankDTO, getCurrentUserEmail()));
    }

    @PostMapping("/{piggyBankId}/deposit")
    public ResponseEntity<PiggyBankDTO> addBalanceToPiggyBank(@PathVariable Long piggyBankId, @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(piggyBankService.addBalanceToPiggyBank(getCurrentUserEmail(), piggyBankId, amount));
    }

    @PostMapping("/{piggyBankId}/withdraw")
    public ResponseEntity<PiggyBankDTO> removeBalanceFromPiggyBank(@PathVariable Long piggyBankId, @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(piggyBankService.removeBalanceFromPiggyBank(getCurrentUserEmail(), piggyBankId, amount));
    }

    @DeleteMapping("/{piggyBankId}")
    public ResponseEntity<Void> deletePiggyBank(@PathVariable Long piggyBankId) {
        piggyBankService.deletePiggyBank(getCurrentUserEmail(), piggyBankId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<PiggyBankDTO>> getAllPiggyBanks() {
        return ResponseEntity.ok(piggyBankService.getAllPiggyBanks(getCurrentUserEmail()));
    }

}
