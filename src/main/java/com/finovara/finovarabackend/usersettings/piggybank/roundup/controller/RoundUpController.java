package com.finovara.finovarabackend.usersettings.piggybank.roundup.controller;

import com.finovara.finovarabackend.piggybank.dto.PiggyBankDTO;
import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersettings.piggybank.roundup.dto.RoundUpDto;
import com.finovara.finovarabackend.usersettings.piggybank.roundup.service.RoundUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/piggybank-settings/round-up")
@RequiredArgsConstructor
public class RoundUpController {

    private final RoundUpService roundUpService;

    @PostMapping
    public ResponseEntity<PiggyBankDTO> addDefaultPiggyBank(@RequestBody PiggyBankDTO piggyBankDTO) {
        return ResponseEntity.ok(roundUpService.addDefaultPiggyBank(piggyBankDTO, SecurityUtils.getCurrentUserEmail()));
    }

    @PutMapping("/{piggyBankId}")
    public ResponseEntity<Void> createRoundUp(@RequestBody RoundUpDto roundUpDto, @PathVariable Long piggyBankId) {
        roundUpService.createRoundUp(roundUpDto, piggyBankId, SecurityUtils.getCurrentUserEmail());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{piggyBankId}")
    public ResponseEntity<RoundUpDto> getRoundUp(@PathVariable Long piggyBankId) {
        return ResponseEntity.ok(roundUpService.getRoundUp(SecurityUtils.getCurrentUserEmail(), piggyBankId));
    }

}
