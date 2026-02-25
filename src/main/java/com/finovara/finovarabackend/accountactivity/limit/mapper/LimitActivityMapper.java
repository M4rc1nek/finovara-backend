package com.finovara.finovarabackend.accountactivity.limit.mapper;

import com.finovara.finovarabackend.accountactivity.limit.dto.LimitActivityDto;
import com.finovara.finovarabackend.accountactivity.limit.model.LimitActivity;
import org.springframework.stereotype.Component;

@Component
public class LimitActivityMapper {

    public LimitActivityDto mapToLimitActivity(LimitActivity activity) {
        return new LimitActivityDto(
                activity.getLimitActivityType(),
                activity.getLimitType(),
                activity.getAmount(),
                activity.getPreviousAmount(),
                activity.getDate()
        );
    }
}
