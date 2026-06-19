package com.finovara.financeservice.config.redis.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "cache")
@Getter
@Setter
public class CacheProperties {

    private Map<String, Long> ttlMinutes = new HashMap<>();
}