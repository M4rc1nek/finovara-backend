package com.finovara.api_gateway.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties(prefix = "security")
@Component
@Getter
@Setter
public class SecurityProperties {

    private List<String> whitelist;
}