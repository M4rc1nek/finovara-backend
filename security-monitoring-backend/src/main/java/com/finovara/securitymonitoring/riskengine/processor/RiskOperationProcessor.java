package com.finovara.securitymonitoring.riskengine.processor;

import com.finovara.securitymonitoring.clientdata.repository.ClientDataRepository;
import com.finovara.securitymonitoring.riskengine.repository.RiskOperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskOperationProcessor {
    private final RiskOperationRepository riskOperationRepository;
    private final ClientDataRepository clientDataRepository;

    @Transactional
    public void deleteAllRiskOperationData() {
        riskOperationRepository.deleteAllInBatch();
        clientDataRepository.deleteAllInBatch();
        log.info("Risk Operation data has been deleted at: {}", LocalDateTime.now());
    }
}
