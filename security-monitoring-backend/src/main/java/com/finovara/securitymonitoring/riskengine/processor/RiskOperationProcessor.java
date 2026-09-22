package com.finovara.securitymonitoring.riskengine.processor;

import com.finovara.securitymonitoring.riskengine.model.RiskOperation;
import com.finovara.securitymonitoring.riskengine.repository.RiskOperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskEngineProcessor {
    private final RiskOperationRepository riskOperationRepository;
    public void deleteAllRiskOperationData(){}
}
