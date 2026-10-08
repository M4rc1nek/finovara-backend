package com.finovara.activitylogservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableScheduling
@EnableFeignClients
@EnableCaching
@SpringBootApplication(scanBasePackages = {"com.finovara.activitylogservice", "com.finovara.contracts.cache"})
public class ActivityLogBackendApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(ActivityLogBackendApplication.class, args);
    }
}
