package com.finovara.financeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableFeignClients
@EnableScheduling
@EntityScan(basePackages = {"com.finovara.financeservice", "com.finovara.contracts.outbox"})
@EnableJpaRepositories(basePackages = {"com.finovara.financeservice", "com.finovara.contracts.outbox"})
@ComponentScan(basePackages = {"com.finovara.financeservice", "com.finovara.contracts.outbox"})
@SpringBootApplication
public class FinanceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinanceServiceApplication.class, args);
	}

}