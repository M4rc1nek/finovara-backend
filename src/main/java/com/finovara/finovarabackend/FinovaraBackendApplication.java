package com.finovara.finovarabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class FinovaraBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinovaraBackendApplication.class, args);
	}

}
