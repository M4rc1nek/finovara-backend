package com.finovara.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableAsync
@EnableScheduling
@EnableFeignClients
@EnableJpaRepositories(basePackages = {"com.finovara.notificationservice", "com.finovara.contracts.outbox"})
@EntityScan(basePackages = {"com.finovara.notificationservice", "com.finovara.contracts.outbox"})
@SpringBootApplication(scanBasePackages = {"com.finovara.notificationservice", "com.finovara.contracts.outbox", "com.finovara.contracts.user.authorization.additionalcode.resolver"})
public class NotificationServiceApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

}