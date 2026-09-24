package com.finovara.securitymonitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableAsync
@EnableFeignClients
@EnableScheduling
@EnableJpaRepositories(basePackages = {"com.finovara.securitymonitoring", "com.finovara.contracts.outbox"})
@EntityScan(basePackages = {"com.finovara.securitymonitoring", "com.finovara.contracts.outbox"})
@SpringBootApplication(scanBasePackages = {"com.finovara.securitymonitoring","com.finovara.contracts.outbox"})
public class SecurityMonitoringBackendApplication {

	public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(SecurityMonitoringBackendApplication.class, args);
	}
}
