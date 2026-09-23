package com.finovara.financeservice.feignclient;

import com.finovara.contracts.securitymonitoring.dto.RiskEvaluationRequest;
import com.finovara.contracts.securitymonitoring.dto.RiskEvaluationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "security-monitoring-backend", url = "${security-monitoring-backend.url}")
public interface SecurityMonitoringClient {

    @PostMapping("/internal/risk/evaluate")
    RiskEvaluationResponse evaluate(@RequestBody RiskEvaluationRequest request);
}