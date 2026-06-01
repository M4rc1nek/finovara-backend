package com.finovara.activityservice.kafka;

import com.finovara.activityservice.activitylog.accountactivity.expense.service.ExpenseActivityService;
import com.finovara.contracts.event.activity.expense.ExpenseActivityEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpenseActivityConsumer {

    private final ExpenseActivityService expenseActivityService;

    @KafkaListener(topics = "activity.expense", groupId = "activity-service")
    public void handle(ExpenseActivityEvent event) {
        expenseActivityService.handleEvent(event);
    }
}
