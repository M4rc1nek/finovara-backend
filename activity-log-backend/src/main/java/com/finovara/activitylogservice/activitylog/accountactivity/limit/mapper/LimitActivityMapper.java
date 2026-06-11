package com.finovara.activitylogservice.activitylog.accountactivity.limit.mapper;

import com.finovara.activitylogservice.activitylog.accountactivity.limit.dto.LimitActivityDto;
import com.finovara.activitylogservice.activitylog.accountactivity.limit.model.LimitActivity;
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
