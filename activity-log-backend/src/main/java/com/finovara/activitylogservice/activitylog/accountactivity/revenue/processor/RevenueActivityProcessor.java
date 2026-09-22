package com.finovara.activitylogservice.activitylog.accountactivity.revenue.processor;

import com.finovara.activitylogservice.activitylog.accountactivity.revenue.repository.RevenueActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class RevenueActivityProcessor {

    private final RevenueActivityRepository revenueActivityRepository;

    @Transactional
    public void deleteRevenueActivity(){
        revenueActivityRepository.deleteAllInBatch();
        log.info("Revenue Activity has been deleted.");
    }
}
