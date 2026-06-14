package com.finovara.authbackend.revenue.mapper;

import com.finovara.authbackend.revenue.dto.RevenueDto;
import com.finovara.authbackend.revenue.model.Revenue;
import org.springframework.stereotype.Component;

@Component
public class RevenueMapper {
    public RevenueDto mapRevenueToDto(Revenue revenue) {
        return new RevenueDto(
                revenue.getId(),
                revenue.getUserId(),
                revenue.getAmount(),
                revenue.getCategory(),
                revenue.getCreatedAt(),
                revenue.getDescription()
        );
    }
}
