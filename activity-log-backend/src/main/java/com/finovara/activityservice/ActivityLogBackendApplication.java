package com.finovara.activityservice;

import com.finovara.contracts.exception.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableFeignClients
@Import(GlobalExceptionHandler.class)
@SpringBootApplication
public class ActivityLogBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivityLogBackendApplication.class, args);
    }
}
