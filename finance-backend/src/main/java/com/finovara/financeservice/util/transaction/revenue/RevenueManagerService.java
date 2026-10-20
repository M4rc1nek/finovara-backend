package com.finovara.financeservice.util.transaction.revenue;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.financeservice.revenue.model.Revenue;
import com.finovara.financeservice.revenue.repository.RevenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RevenueManagerService {
    private final RevenueRepository revenueRepository;

    public Revenue getRevenueByUserIdOrThrow(Long revenueId, Long userId) {
        return revenueRepository.findByIdAndUserId(revenueId, userId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Revenue not found"));
    }

}

