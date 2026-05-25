package com.finovara.corebackend;

import com.finovara.corebackend.security.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableAsync
@EnableKafka
@EnableScheduling
@EnableFeignClients
@EnableConfigurationProperties(SecurityProperties.class)
@SpringBootApplication(scanBasePackages = {
        "com.finovara.contracts-backend"
})
public class FinovaraBackendApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(FinovaraBackendApplication.class, args);
    }
}