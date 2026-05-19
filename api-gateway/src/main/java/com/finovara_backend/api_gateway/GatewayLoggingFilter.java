package com.finovara_backend.api_gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
public class GatewayLoggingFilter {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Bean
    public GlobalFilter loggingFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            String method = exchange.getRequest().getMethod().name();

            return chain.filter(exchange).doOnSuccess(v -> {
                Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
                String routeId = route != null ? route.getId() : "unknown";

                log.info("#{} Api-gateway redirected to: {} {} → [{}]", COUNTER.incrementAndGet(), method, path, routeId);
            });
        };
    }
}
