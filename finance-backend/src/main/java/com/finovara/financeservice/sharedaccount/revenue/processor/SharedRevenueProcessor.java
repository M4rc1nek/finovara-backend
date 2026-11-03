package com.finovara.financeservice.sharedaccount.revenue.processor;

import com.finovara.financeservice.sharedaccount.revenue.repository.SharedRevenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SharedRevenueProcessor {

    private final SharedRevenueRepository sharedRevenueRepository;

    @Transactional
    public void deleteAllSharedRevenues() {
        sharedRevenueRepository.deleteAllInBatch();
        log.info("Shared revenues have been deleted.");
    }
}
