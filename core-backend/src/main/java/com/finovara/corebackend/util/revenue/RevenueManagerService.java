package com.finovara.corebackend.util.revenue;

import com.finovara.corebackend.revenue.exception.notfound.RevenueNotFoundException;
import com.finovara.corebackend.revenue.model.Revenue;
import com.finovara.corebackend.revenue.repository.RevenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RevenueManagerService {
    private final RevenueRepository revenueRepository;

    public Revenue getRevenueOrThrow(Long revenueId) {
        return revenueRepository.findById(revenueId)
                .orElseThrow(() -> new RevenueNotFoundException("Revenue not found"));
    }

}
