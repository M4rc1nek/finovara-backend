package com.finovara.activityservice.activity_log.accountactivity.limit.mapper;

import com.finovara.activityservice.activity_log.accountactivity.limit.dto.LimitActivityDto;
import com.finovara.activityservice.activity_log.accountactivity.limit.model.LimitActivity;
import org.springframework.stereotype.Component;

@Component
public class LimitActivityMapper {

    public LimitActivityDto mapToLimitActivity(LimitActivity activity) {
        return new LimitActivityDto(
                activity.getLimitActivityType(),
                activity.getPeriodType(),
                activity.getAmount(),
                activity.getPreviousAmount(),
                activity.getCreatedAt()
        );
    }
}
