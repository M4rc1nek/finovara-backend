package com.finovara.notificationservice.consumer.notificationemail;

import com.finovara.contracts.mainaccount.activity.event.secure.login.activity.LoginActivityEvent;
import com.finovara.notificationservice.notificationemail.service.login.NewLoginNotificationHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NewLoginNotificationConsumer {

    private final NewLoginNotificationHandler newLoginNotificationHandler;

    @KafkaListener(topics = "user.logged-in", groupId = "notification-email-service")
    public void consume(LoginActivityEvent event) {
        newLoginNotificationHandler.handle(event);
    }
}
