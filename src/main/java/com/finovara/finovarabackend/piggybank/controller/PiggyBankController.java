package com.finovara.finovarabackend.piggybank.controller;

import com.finovara.finovarabackend.piggybank.dto.PiggyBankDTO;
import com.finovara.finovarabackend.piggybank.service.PiggyBankService;
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
public class PiggyBankController {
    private final PiggyBankService piggyBankService;

    @PostMapping
    public ResponseEntity<Long> createPiggyBank(@RequestBody @Valid PiggyBankDTO piggyBankDTO) {
        return ResponseEntity.ok(piggyBankService.addPiggyBank(piggyBankDTO, getCurrentUserEmail()));
    }

    @PatchMapping("/{piggyBankId}")
    public ResponseEntity<Long> editPiggyBank(@RequestBody @Valid PiggyBankDTO piggyBankDTO, @PathVariable Long piggyBankId) {
        return ResponseEntity.ok(piggyBankService.editPiggyBank(SecurityUtils.getCurrentUserEmail(), piggyBankDTO, piggyBankId));
    }

    @GetMapping
    public ResponseEntity<List<PiggyBankDTO>> getAllPiggyBanks() {
        return ResponseEntity.ok(piggyBankService.getAllPiggyBanks(getCurrentUserEmail()));
    }

    @DeleteMapping("/{piggyBankId}")
    public ResponseEntity<Void> deletePiggyBank(@PathVariable Long piggyBankId) {
        piggyBankService.deletePiggyBank(getCurrentUserEmail(), piggyBankId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{piggyBankId}/deposit")
    public ResponseEntity<Void> addBalanceToPiggyBank(@PathVariable Long piggyBankId, @RequestParam BigDecimal amount) {
        piggyBankService.addBalanceToPiggyBank(getCurrentUserEmail(), piggyBankId, amount);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{piggyBankId}/withdraw")
    public ResponseEntity<PiggyBankDTO> removeBalanceFromPiggyBank(@PathVariable Long piggyBankId, @RequestParam BigDecimal amount) {
        piggyBankService.removeBalanceFromPiggyBank(getCurrentUserEmail(), piggyBankId, amount);
        return ResponseEntity.noContent().build();
    }

}
