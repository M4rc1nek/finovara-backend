package com.finovara.finovarabackend.revenue.mapper;

import com.finovara.finovarabackend.revenue.dto.RevenueDTO;
import com.finovara.finovarabackend.revenue.model.Revenue;
import org.springframework.stereotype.Component;

@Component
public class RevenueMapper {
    public RevenueDTO mapRevenueToDTO(Revenue revenue) {
        return new RevenueDTO(
                revenue.getId(),
                revenue.getUserAssigned().getId(),
                revenue.getAmount(),
                revenue.getCategory(),
                revenue.getCreatedAt(),
                revenue.getDescription()
        );
    }
}
