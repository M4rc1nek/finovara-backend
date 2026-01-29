package com.finovara.finovarabackend.usersettings.piggybank.roundup.controller;

import com.finovara.finovarabackend.piggybank.dto.PiggyBankDTO;
import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersettings.piggybank.roundup.dto.RoundUpDto;
import com.finovara.finovarabackend.usersettings.piggybank.roundup.service.RoundUpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance-roundup")
@RequiredArgsConstructor
public class RoundUpController {

    private final RoundUpService roundUpService;

    @PostMapping
    public ResponseEntity<PiggyBankDTO> addDefaultPiggyBank(@RequestBody @Valid PiggyBankDTO piggyBankDTO) {
        return ResponseEntity.ok(roundUpService.addDefaultPiggyBank(piggyBankDTO, SecurityUtils.getCurrentUserEmail()));
    }

    @PutMapping("/{piggyBankId}")
    public ResponseEntity<Void> createRoundUp(@RequestBody @Valid RoundUpDto roundUpDto, @PathVariable Long piggyBankId) {
        roundUpService.createRoundUp(roundUpDto, piggyBankId, SecurityUtils.getCurrentUserEmail());
        return ResponseEntity.noContent().build();
    }

    @PutMapping
    public ResponseEntity<Void>updateRoundUpSettings(@RequestBody List<RoundUpDto> settings){
        roundUpService.updatePiggyBank(SecurityUtils.getCurrentUserEmail(), settings);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{piggyBankId}")
    public ResponseEntity<RoundUpDto> getRoundUp(@PathVariable Long piggyBankId) {
        return ResponseEntity.ok(roundUpService.getRoundUp(SecurityUtils.getCurrentUserEmail(), piggyBankId));
    }

}
