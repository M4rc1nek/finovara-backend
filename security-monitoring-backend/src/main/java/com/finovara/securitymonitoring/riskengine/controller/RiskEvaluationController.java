package com.finovara.securitymonitoring.riskengine.controller;

import com.finovara.contracts.securitymonitoring.dto.RiskEvaluationRequest;
import com.finovara.contracts.securitymonitoring.dto.RiskEvaluationResponse;
import com.finovara.contracts.logging.InvocationTimeLogger;
import com.finovara.securitymonitoring.riskengine.service.RiskEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/risk")
@RequiredArgsConstructor
public class RiskEvaluationController {

    private final RiskEngineService riskEngineService;

    @PostMapping("/evaluate")
    @InvocationTimeLogger
    public RiskEvaluationResponse evaluate(@RequestBody RiskEvaluationRequest request) {
        return riskEngineService.evaluate(request);
    }
}