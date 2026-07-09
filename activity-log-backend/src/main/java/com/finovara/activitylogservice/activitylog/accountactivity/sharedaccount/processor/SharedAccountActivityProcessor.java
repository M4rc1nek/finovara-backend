package com.finovara.activitylogservice.activitylog.accountactivity.revenue.processor;

import com.finovara.activitylogservice.activitylog.accountactivity.revenue.repository.RevenueActivityRepository;
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
