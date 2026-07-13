package com.finovara.financeservice.sharedaccount.revenue.mapper;

import com.finovara.financeservice.sharedaccount.revenue.dto.SharedRevenueDto;
import com.finovara.financeservice.sharedaccount.revenue.model.SharedRevenue;
import org.springframework.stereotype.Component;

@Component
public class SharedRevenueMapper {

    public SharedRevenueDto mapToDto(SharedRevenue revenue, String createdByUsername) {
        return new SharedRevenueDto(
                revenue.getId(),
                revenue.getAmount(),
                revenue.getCategory(),
                revenue.getCreatedAt(),
                revenue.getDescription(),
                revenue.getCreatedByUserId(),
                createdByUsername
        );
    }
}