package com.finovara.financeservice.revenue.mapper;

import com.finovara.financeservice.revenue.dto.RevenueDto;
import com.finovara.financeservice.revenue.model.Revenue;
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
                revenue.getDescription(),
                null
        );
    }
}
