package com.finovara.apigateway.ratelimit.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finovara.apigateway.ratelimit.RateLimitMessage;
import com.finovara.contracts.exception.ErrorResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final int TOO_MANY_REQUESTS = 429;
    private static final String RATE_LIMIT_PREFIX = "rate-limit:";

    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        Optional<RateLimitProperties.Endpoint> endpointOpt = findEndpoint(path);

        if (endpointOpt.isEmpty()) {
            return chain.filter(exchange);
        }

        String ip = resolveIp(exchange.getRequest());
        RateLimitProperties.Endpoint endpoint = endpointOpt.get();
        String key = RATE_LIMIT_PREFIX + endpoint.getPath() + ":" + ip;
        Duration window = endpoint.getWindowMinutes() > 0 ? Duration.ofMinutes(endpoint.getWindowMinutes()) : Duration.ofHours(endpoint.getWindowHours());

        return reactiveRedisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        return reactiveRedisTemplate.expire(key, window)
                                .thenReturn(count);
                    }
                    return Mono.just(count);
                })
                .flatMap(count -> {
                    if (count > endpoint.getMaxRequests()) {
                        return writeTooManyRequestsResponse(exchange, path);
                    }
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return -2;
    }

    private Optional<RateLimitProperties.Endpoint> findEndpoint(String path) {
        return rateLimitProperties.getEndpoints().stream()
                .filter(e -> StringUtils.hasText(e.getPath()))
                .filter(e -> pathMatcher.match(e.getPath(), path))
                .findFirst();
    }

    private String resolveIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }

    private Mono<Void> writeTooManyRequestsResponse(ServerWebExchange exchange, String path) {
        try {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

            ErrorResponseDto errorResponse = new ErrorResponseDto(
                    TOO_MANY_REQUESTS,
                    "Too Many Requests",
                    RateLimitMessage.TRY_AGAIN_IN_1HOUR.label(),
                    path
            );

            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));

        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return exchange.getResponse().setComplete();
        }
    }
}