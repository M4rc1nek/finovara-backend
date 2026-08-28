package com.finovara.financeservice.util.transaction.revenue;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenue;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SharedRevenueManagerService {
    private final SharedRevenueRepository sharedRevenueRepository;

    public SharedRevenue getSharedRevenueOrThrow(Long revenueId) {
        return sharedRevenueRepository.findById(revenueId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Revenue not found"));
    }
}
