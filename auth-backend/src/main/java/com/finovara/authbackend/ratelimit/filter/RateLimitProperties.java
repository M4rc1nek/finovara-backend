package com.finovara.authbackend.ratelimit.filter;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private List<Endpoint> endpoints = new ArrayList<>();

    @Getter
    @Setter
    public static class Endpoint {

        private String path;
        private int maxRequests = 3;
        private int windowHours = 1;

    }
}
