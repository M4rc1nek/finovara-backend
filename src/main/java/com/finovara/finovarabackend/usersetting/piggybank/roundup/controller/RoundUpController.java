package com.finovara.finovarabackend.usersetting.piggybank.roundup.controller;

import com.finovara.finovarabackend.piggybank.dto.PiggyBankDTO;
import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersetting.piggybank.roundup.dto.RoundUpDto;
import com.finovara.finovarabackend.usersetting.piggybank.roundup.service.RoundUpService;
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

    @GetMapping
    public ResponseEntity<RoundUpDto> getRoundUp() {
        return ResponseEntity.ok(roundUpService.getRoundUp(SecurityUtils.getCurrentUserEmail()));
    }

}
