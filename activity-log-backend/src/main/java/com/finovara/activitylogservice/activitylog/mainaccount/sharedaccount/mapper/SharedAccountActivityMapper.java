package com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.mapper;

import com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.dto.SharedAccountChangeHistoryActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.sharedaccount.model.SharedAccountChangeHistoryActivity;
import org.springframework.stereotype.Component;

@Component
public class SharedAccountActivityMapper {

    public SharedAccountChangeHistoryActivityDto mapToSharedAccountActivity(SharedAccountChangeHistoryActivity activity) {
        return new SharedAccountChangeHistoryActivityDto(
                activity.getType(),
                activity.getRefundedBalance(),
                activity.getCoFounderUsername(),
                activity.getCoFounderEmail(),
                activity.getCreatedAt()
        );
    }
}
