package com.finovara.activitylogservice.activitylog.mainaccountactivity.expense.mapper;

import com.finovara.activitylogservice.activitylog.mainaccountactivity.expense.dto.ExpenseActivityDto;
import com.finovara.activitylogservice.activitylog.mainaccountactivity.expense.model.ExpenseActivity;
import org.springframework.stereotype.Component;

@Component
public class ExpenseActivityMapper {

    public ExpenseActivityDto mapToExpenseActivity(ExpenseActivity activity) {
        return new ExpenseActivityDto(
                activity.getType(),
                activity.getAmount(),
                activity.getPreviousAmount(),
                activity.getCategory(),
                activity.getPreviousCategory(),
                activity.getCreatedAt()
        );

    }

}
