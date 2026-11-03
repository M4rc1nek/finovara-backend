package com.finovara.financeservice.revenue.processor;

import com.finovara.financeservice.revenue.repository.RevenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RevenueProcessor {

    private final RevenueRepository revenueRepository;

    @Transactional
    public void deleteAllRevenues() {
        revenueRepository.deleteAllInBatch();
        log.info("Revenues have been deleted.");
    }
}
