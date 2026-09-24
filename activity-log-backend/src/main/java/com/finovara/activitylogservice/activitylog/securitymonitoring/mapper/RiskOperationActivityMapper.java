package com.finovara.activitylogservice.activitylog.securitymonitoring.mapper;

import com.finovara.activitylogservice.activitylog.securitymonitoring.dto.RiskOperationActivityDto;
import com.finovara.activitylogservice.activitylog.securitymonitoring.model.RiskOperationActivity;
import com.finovara.activitylogservice.activitylog.securitymonitoring.model.RiskRuleCollectionActivity;
import org.springframework.stereotype.Component;

@Component
public class RiskOperationActivityMapper {

    public RiskOperationActivityDto mapToRiskOperationActivity(RiskOperationActivity activity) {
        return new RiskOperationActivityDto(
                activity.getSourceEventId(),
                activity.getTriggerType(),
                activity.getScore(),
                activity.getAction(),
                activity.getRiskRuleCollectionActivities().stream()
                        .map(RiskRuleCollectionActivity::getRiskRule)
                        .toList(),
                activity.getCreatedAt(),
                activity.getOperationDate());
    }
}
