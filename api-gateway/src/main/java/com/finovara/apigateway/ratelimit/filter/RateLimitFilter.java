package com.finovara.apigateway.ratelimit.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finovara.apigateway.ratelimit.RateLimitMessage;
import com.finovara.contracts.exception.ErrorResponseDto;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final int TOO_MANY_REQUESTS = 429;
    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        Optional<RateLimitProperties.Endpoint> endpoint = findEndpoint(path);

        if (endpoint.isEmpty()) {
            return chain.filter(exchange);
        }

        String ip = resolveIp(exchange.getRequest());
        RateLimitProperties.Endpoint rateLimitEndpoint = endpoint.get();
        String bucketKey = rateLimitEndpoint.getPath() + ":" + ip;
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(rateLimitEndpoint));

        if (!bucket.tryConsume(1)) {
            return writeTooManyRequestsResponse(exchange, path);
        }

        return chain.filter(exchange);
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

    private Bucket createBucket(RateLimitProperties.Endpoint endpoint) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(endpoint.getMaxRequests())
                        .refillGreedy(endpoint.getMaxRequests(), Duration.ofHours(endpoint.getWindowHours()))
                        .build())
                .build();
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