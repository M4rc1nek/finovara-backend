package com.finovara.financeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableFeignClients
@EnableScheduling
@EntityScan(basePackages = {"com.finovara.financeservice", "com.finovara.contracts.outbox"})
@EnableJpaRepositories(basePackages = {"com.finovara.financeservice", "com.finovara.contracts.outbox"})
@SpringBootApplication(scanBasePackages = {"com.finovara.financeservice", "com.finovara.contracts.outbox", "com.finovara.contracts.cache", "com.finovara.contracts.authorization.additionalcode.resolver"})
public class FinanceServiceApplication {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(FinanceServiceApplication.class, args);
    }
}