package com.finovara.activitylogservice.activitylog.securitymonitoring.processor;

import com.finovara.activitylogservice.activitylog.securitymonitoring.repository.RiskOperationActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class RiskOperationActivityProcessor {

    private final RiskOperationActivityRepository riskOperationActivityRepository;

    @Transactional
    public void deleteRiskOperationActivity() {
        riskOperationActivityRepository.deleteAllInBatch();
        log.info("Risk operation activity has been deleted.");
    }
}
