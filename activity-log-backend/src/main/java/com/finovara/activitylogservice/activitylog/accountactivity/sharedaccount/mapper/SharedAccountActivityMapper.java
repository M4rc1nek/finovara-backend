package com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.mapper;

import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.dto.SharedAccountActivityDto;
import com.finovara.activitylogservice.activitylog.accountactivity.sharedaccount.model.SharedAccountActivity;
import org.springframework.stereotype.Component;

@Component
public class SharedAccountActivityMapper {

    public SharedAccountActivityDto mapToSharedAccountActivity(SharedAccountActivity activity) {
        return new SharedAccountActivityDto(
                activity.getType(),
                activity.getRefundedBalance(),
                activity.getCoFounderUsername(),
                activity.getCoFounderEmail(),
                activity.getCreatedAt()
        );
    }
}
