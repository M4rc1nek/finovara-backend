package com.finovara.activityservice.kafka;

import com.finovara.activityservice.activity_log.accountactivity.secure.login.activity.service.LoginActivityService;
import com.finovara.activityservice.contracts.event.secure.login.activity.LoginActivityEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginActivityConsumer {

    private final LoginActivityService loginActivityService;

    @KafkaListener(topics = "activity.login", groupId = "activity-service")
    public void handle(LoginActivityEvent event) {
        loginActivityService.handleEvent(event);
    }
}
