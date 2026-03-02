package com.finovara.finovarabackend.usersetting.finances.revenue.scoring.controller;

import com.finovara.finovarabackend.security.SecurityUtils;
import com.finovara.finovarabackend.usersetting.finances.revenue.scoring.dto.RevenueScoringDto;
import com.finovara.finovarabackend.usersetting.finances.revenue.scoring.service.RevenueScoringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/revenue-settings/revenue-scoring")
@RequiredArgsConstructor
public class RevenueScoringController {

    private final RevenueScoringService revenueScoringService;

    @PatchMapping
    public ResponseEntity<Void> saveScoringIncome(@RequestBody @Valid RevenueScoringDto dto){
        revenueScoringService.saveScoringIncome(SecurityUtils.getCurrentUserEmail(), dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<RevenueScoringDto> getScoringIncome() {
        return ResponseEntity.ok(revenueScoringService.getScoringIncome(SecurityUtils.getCurrentUserEmail()));
    }
}
