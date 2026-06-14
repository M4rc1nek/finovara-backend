package com.finovara.authbackend.util.revenue;

import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import com.finovara.authbackend.revenue.model.Revenue;
import com.finovara.authbackend.revenue.repository.RevenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RevenueManagerService {
    private final RevenueRepository revenueRepository;

    public Revenue getRevenueOrThrow(Long revenueId) {
        return revenueRepository.findById(revenueId)
                .orElseThrow(() -> new RequestedEntityNotFoundException("Revenue not found"));
    }

}
