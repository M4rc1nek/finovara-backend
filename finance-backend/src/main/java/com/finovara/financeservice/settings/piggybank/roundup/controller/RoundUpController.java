package com.finovara.financeservice.settings.piggybank.roundup.controller;

import com.finovara.financeservice.security.SecurityUtils;
import com.finovara.financeservice.settings.piggybank.roundup.dto.RoundUpDto;
import com.finovara.financeservice.settings.piggybank.roundup.service.RoundUpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/piggybank-settings/round-up")
@RequiredArgsConstructor
public class RoundUpController {

    private final RoundUpService roundUpService;

    @GetMapping("/{piggyBankId}")
    public ResponseEntity<RoundUpDto> getRoundUp(@PathVariable Long piggyBankId) {
        return ResponseEntity.ok(roundUpService.getRoundUp(SecurityUtils.getCurrentUserId(), piggyBankId));
    }

    @PatchMapping("/{piggyBankId}")
    public ResponseEntity<Void> saveRoundUpPiggyBank(@RequestBody @Valid RoundUpDto roundUpDto, @PathVariable Long piggyBankId) {
        roundUpService.saveRoundUpPiggyBank(SecurityUtils.getCurrentUserId(), piggyBankId, roundUpDto);
        return ResponseEntity.noContent().build();
    }

}
