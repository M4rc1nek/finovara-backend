package com.finovara.activitylogservice.activitylog.mainaccount.piggybank.mapper;

import com.finovara.activitylogservice.activitylog.mainaccount.piggybank.dto.PiggyBankActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccount.piggybank.model.PiggyBankActivity;
import org.springframework.stereotype.Component;

@Component
public class PiggyBankActivityMapper {
    public PiggyBankActivityDto mapToPiggyBankActivity(PiggyBankActivity activity) {
        return new PiggyBankActivityDto(
                activity.getPiggyBankName(),
                activity.getPreviousPiggyBankName(),
                activity.getActivityType(),
                activity.getGoalType(),
                activity.getPreviousGoalType(),
                activity.getGoalAmount(),
                activity.getPreviousGoalAmount(),
                activity.getAmountPaid(),
                activity.getAmountPaidOut(),
                activity.getCreatedAt()
        );
    }
}
