package com.finovara.notificationservice.notificationemail.sharedaccount.piggybank.goalachieved.consumer;

import com.finovara.contracts.event.finance.sharedaccount.GoalAchievedNotificationEvent;
import com.finovara.notificationservice.notificationemail.sharedaccount.piggybank.goalachieved.service.GoalAchievedNotificationHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoalAchievedNotificationConsumer {

    private final GoalAchievedNotificationHandler goalAchievedNotificationHandler;

    @KafkaListener(topics = "notification.shared-account.piggy-bank-goal-achieved", groupId = "notification-email-service")
    public void consume(GoalAchievedNotificationEvent event) {
        goalAchievedNotificationHandler.handle(event);
    }

}