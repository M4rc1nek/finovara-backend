package com.finovara.activitylogservice.activitylog.mainaccount.revenue.mapper;

import com.finovara.activitylogservice.activitylog.mainaccount.revenue.dto.RevenueActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.revenue.model.RevenueActivity;
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
                activity.getCreatedAt()
        );
    }
}
