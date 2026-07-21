package com.finovara.financeservice.sharedaccount.settings.factory;

import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettings;
import com.finovara.financeservice.sharedaccount.settings.SharedAccountSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class SharedAccountSettingsFactory {

    private final SharedAccountSettingsRepository sharedAccountSettingsRepository;

    @Transactional
    public void createDefaultSharedAccountSettingsIfNotExist(Long ownerId, Long memberId) {
        try {
            sharedAccountSettingsRepository.save(SharedAccountSettings.builder()
                    .spendControlEnabled(false)
                    .spendControlPercentage(BigDecimal.ZERO)
                    .expenseAnalysisEnabled(false)
                    .ownerId(ownerId)
                    .memberId(memberId)
                    .build());
            log.info("Shared Account settings created for ownerId={}, memberId={}", ownerId, memberId);
        } catch (DataIntegrityViolationException e) {
            log.debug("Shared Account  settings already exist for ownerId={}, memberId={}", ownerId, memberId);
        }
    }
}
