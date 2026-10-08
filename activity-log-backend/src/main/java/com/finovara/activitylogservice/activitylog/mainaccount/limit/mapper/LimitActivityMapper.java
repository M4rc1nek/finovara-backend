package com.finovara.activitylogservice.activitylog.mainaccountactivity.limit.mapper;

import com.finovara.activitylogservice.activitylog.mainaccountactivity.limit.dto.LimitActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccountactivity.limit.model.LimitActivity;
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
