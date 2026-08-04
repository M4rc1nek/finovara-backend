package com.finovara.authservice.util.attempts.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "verification-code-manager")
public class VerificationCodeProperties {
    private int codeExpirationMinutes = 15;
    private int attemptsExpirationMinutes = 2;
    private int maxAttempts = 5;
}
