package com.finovara.securitymonitoring.riskchallenge.controller;

import com.finovara.contracts.authorization.dto.ConfirmPasswordDto;
import com.finovara.securitymonitoring.riskchallenge.dto.ChallengeConfirmationResponse;
import com.finovara.securitymonitoring.riskchallenge.dto.ConfirmEmailCodeDto;
import com.finovara.securitymonitoring.riskchallenge.service.RiskChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/risk")
@RequiredArgsConstructor
public class RiskChallengeController {

    private final RiskChallengeService riskChallengeService;

    @PostMapping("/confirm-password/{riskOperationId}")
    public ChallengeConfirmationResponse confirmPassword(@RequestBody ConfirmPasswordDto dto, @PathVariable Long riskOperationId) {
        return riskChallengeService.confirmPassword(riskOperationId, dto.password());
    }

    @PostMapping("/confirm-email-code/{riskOperationId}")
    public ChallengeConfirmationResponse confirmEmailCode(@RequestBody ConfirmEmailCodeDto dto, @PathVariable Long riskOperationId) {
        return riskChallengeService.confirmEmailCode(riskOperationId, dto.code());
    }
}