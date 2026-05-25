package com.finovara.activityservice.activity_log.accountactivity.revenue.processor;

import com.finovara.activityservice.activity_log.accountactivity.revenue.repository.RevenueActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RevenueActivityProcessor {

    private final RevenueActivityRepository revenueActivityRepository;

    public void deleteRevenueActivity(){
        revenueActivityRepository.deleteAllInBatch();
    }
}
