package com.finovara.securitymonitoring.transaction.factory;

import com.finovara.securitymonitoring.transaction.model.TransactionProfile;
import com.finovara.securitymonitoring.transaction.repository.TransactionProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionProfileFactory {

    private final TransactionProfileRepository transactionProfileRepository;

    @Transactional
    public void createDefaultDataIfNotExist(Long userId) {
        if (transactionProfileRepository.existsByUserId(userId)) {
            log.debug("Transaction profile already exists for userId={}, skipping", userId);
            return;
        }

        TransactionProfile profile = TransactionProfile.builder()
                .expenseCount(0L)
                .revenueCount(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .userId(userId)
                .build();

        transactionProfileRepository.save(profile);
        log.info("Default transaction profile created for userId={}", userId);
    }
}