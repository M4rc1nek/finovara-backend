package com.finovara.finovarabackend.accountactivity.revenue.mapper;

import com.finovara.finovarabackend.accountactivity.revenue.dto.RevenueActivityDto;
import com.finovara.finovarabackend.accountactivity.revenue.model.RevenueActivity;
import org.springframework.stereotype.Component;

@Component
public class RevenueActivityMapper {

    public RevenueActivityDto mapToRevenueActivity(RevenueActivity activity) {
        return new RevenueActivityDto(
                activity.getType(),
                activity.getAmount(),
                activity.getPreviousAmount(),
                activity.getCategory(),
                activity.getPreviousCategory(),
                activity.getDate()
        );
    }
}
