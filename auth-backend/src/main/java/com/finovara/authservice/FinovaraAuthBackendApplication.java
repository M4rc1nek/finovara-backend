package com.finovara.authservice;

import com.finovara.authservice.security.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableAsync
@EnableKafka
@EnableFeignClients
@EnableScheduling
@EnableConfigurationProperties(SecurityProperties.class)
@SpringBootApplication
@EntityScan(basePackages = {"com.finovara.authservice", "com.finovara.contracts.outbox"})
@EnableJpaRepositories(basePackages = {"com.finovara.authservice", "com.finovara.contracts.outbox"})
@ComponentScan(basePackages = {"com.finovara.authservice", "com.finovara.contracts.outbox", "com.finovara.contracts.authorization.additionalcode.resolver"})
public class FinovaraAuthBackendApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(FinovaraAuthBackendApplication.class, args);
    }
}