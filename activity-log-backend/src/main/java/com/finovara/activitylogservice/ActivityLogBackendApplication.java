package com.finovara.activitylogservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableFeignClients
@SpringBootApplication
public class ActivityLogBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivityLogBackendApplication.class, args);
    }
}
