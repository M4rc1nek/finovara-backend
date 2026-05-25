package com.finovara.corebackend.revenue.mapper;

import com.finovara.corebackend.revenue.dto.RevenueDto;
import com.finovara.corebackend.revenue.model.Revenue;
import org.springframework.stereotype.Component;

@Component
public class RevenueMapper {
    public RevenueDto mapRevenueToDto(Revenue revenue) {
        return new RevenueDto(
                revenue.getId(),
                revenue.getUserAssigned().getId(),
                revenue.getAmount(),
                revenue.getCategory(),
                revenue.getCreatedAt(),
                revenue.getDescription()
        );
    }
}
