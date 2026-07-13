package com.finovara.financeservice.sharedaccount.mapper.revenue;

import com.finovara.financeservice.sharedaccount.dto.revenue.SharedRevenueDto;
import com.finovara.financeservice.sharedaccount.model.revenue.SharedRevenue;
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