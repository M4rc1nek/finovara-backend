package com.finovara.finovarabackend.contact.filter;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "contact.rate-limit")
@Getter
@Setter
public class ContactRateLimitProperties {

    private int maxRequests = 3;
    private int windowHours = 1;

}