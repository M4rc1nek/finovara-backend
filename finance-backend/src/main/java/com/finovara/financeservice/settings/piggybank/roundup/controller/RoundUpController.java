package com.finovara.authbackend.usersetting.piggybank.roundup.controller;

import com.finovara.authbackend.piggybank.dto.PiggyBankDto;
import com.finovara.authbackend.security.SecurityUtils;
import com.finovara.authbackend.usersetting.piggybank.roundup.dto.RoundUpDto;
import com.finovara.authbackend.usersetting.piggybank.roundup.service.RoundUpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/piggybank-settings/round-up")
@RequiredArgsConstructor
public class RoundUpController {

    private final RoundUpService roundUpService;

    @PostMapping
    public ResponseEntity<Long> addDefaultPiggyBank(@RequestBody @Valid PiggyBankDto piggyBankDto) {
        return ResponseEntity.ok(roundUpService.addDefaultPiggyBank(piggyBankDto, SecurityUtils.getCurrentUserId()));
    }

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
