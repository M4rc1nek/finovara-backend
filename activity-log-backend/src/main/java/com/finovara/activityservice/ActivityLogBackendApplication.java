package com.finovara.activityservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableKafka
@EnableScheduling
@SpringBootApplication
public class ActivityLogBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivityLogBackendApplication.class, args);
    }
}
