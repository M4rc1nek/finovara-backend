package com.finovara.apigateway.ratelimit.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finovara.apigateway.ratelimit.RateLimitMessage;
import com.finovara.contracts.exception.ErrorResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    private RateLimitFilter filter;
    private RateLimitProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GatewayFilterChain chain = mock(GatewayFilterChain.class);

    @BeforeEach
    void setUp() {
        when(chain.filter(any())).thenReturn(Mono.empty());
        properties = new RateLimitProperties();
        filter = new RateLimitFilter(properties, objectMapper, redisTemplate);
    }

    private void stubRedisCounter(long... counts) {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        AtomicLong callIndex = new AtomicLong(0);
        when(valueOperations.increment(anyString())).thenAnswer(inv -> {
            long idx = callIndex.getAndIncrement();
            long count = idx < counts.length ? counts[(int) idx] : counts[counts.length - 1];
            return Mono.just(count);
        });
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));
    }

    @Test
    void shouldPassThroughWhenPathNotConfigured() {
        properties.setEndpoints(List.of());

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/anything")
                        .remoteAddress(new InetSocketAddress("1.1.1.1", 0))
                        .build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldAllowRequestWhenUnderLimit() {
        stubRedisCounter(1L);
        properties.setEndpoints(List.of(endpointFor("/api/test", 3, 1, 0)));

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .remoteAddress(new InetSocketAddress("1.1.1.1", 0))
                        .build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void shouldReturn429WhenLimitExceeded() throws Exception {
        stubRedisCounter(1L, 2L);
        properties.setEndpoints(List.of(endpointFor("/api/test", 1, 1, 0)));

        var first = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .remoteAddress(new InetSocketAddress("1.1.1.1", 0))
                        .build());
        var second = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .remoteAddress(new InetSocketAddress("1.1.1.1", 0))
                        .build());

        filter.filter(first, chain).block();
        filter.filter(second, chain).block();

        assertThat(second.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        byte[] bodyBytes = second.getResponse()
                .getBody()
                .next()
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    return bytes;
                })
                .block();

        ErrorResponseDto body = objectMapper.readValue(bodyBytes, ErrorResponseDto.class);
        assertThat(body.status()).isEqualTo(429);
        assertThat(body.error()).isEqualTo("Too Many Requests");
        assertThat(body.message()).isEqualTo(RateLimitMessage.TRY_AGAIN_IN_1HOUR.label());
        assertThat(body.path()).isEqualTo("/api/test");

        verify(chain, times(1)).filter(any());
    }

    @Test
    void shouldTrackCountersSeparatelyPerIp() {
        stubRedisCounter(1L, 2L, 1L);
        properties.setEndpoints(List.of(endpointFor("/api/test", 1, 1, 0)));

        var ip1first = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .remoteAddress(new InetSocketAddress("1.1.1.1", 0))
                        .build());

        var ip1second = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .remoteAddress(new InetSocketAddress("1.1.1.1", 0))
                        .build());

        var ip2first = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .remoteAddress(new InetSocketAddress("2.2.2.2", 0))
                        .build());

        filter.filter(ip1first, chain).block();
        filter.filter(ip1second, chain).block();
        filter.filter(ip2first, chain).block();

        assertThat(ip1second.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(ip2first.getResponse().getStatusCode()).isNull();
    }

    @Test
    void shouldMatchAntStyleWildcardPath() {
        stubRedisCounter(1L);
        properties.setEndpoints(List.of(endpointFor("/api/pdf/**", 5, 1, 0)));

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/pdf/reports/123")
                        .remoteAddress(new InetSocketAddress("1.1.1.1", 0))
                        .build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    @Test
    void shouldUseXForwardedForInsteadOfRemoteAddress() {
        stubRedisCounter(1L, 2L);
        properties.setEndpoints(List.of(endpointFor("/api/test", 1, 1, 0)));

        var first = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .remoteAddress(new InetSocketAddress("9.9.9.9", 0))
                        .header("X-Forwarded-For", "5.5.5.5")
                        .build());

        var second = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .remoteAddress(new InetSocketAddress("9.9.9.9", 0))
                        .header("X-Forwarded-For", "5.5.5.5")
                        .build());

        filter.filter(first, chain).block();
        filter.filter(second, chain).block();

        assertThat(second.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void shouldSetTtlOnlyOnFirstRequest() {
        stubRedisCounter(1L, 2L);
        properties.setEndpoints(List.of(endpointFor("/api/test", 5, 1, 0)));

        var first = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .remoteAddress(new InetSocketAddress("1.1.1.1", 0))
                        .build());

        var second = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .remoteAddress(new InetSocketAddress("1.1.1.1", 0))
                        .build());

        filter.filter(first, chain).block();
        filter.filter(second, chain).block();

        verify(redisTemplate, times(1)).expire(anyString(), eq(Duration.ofHours(1)));
    }

    @Test
    void shouldUseWindowMinutesWhenSet() {
        stubRedisCounter(1L);
        properties.setEndpoints(List.of(endpointFor("/api/test", 5, 0, 20)));

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .remoteAddress(new InetSocketAddress("1.1.1.1", 0))
                        .build());

        filter.filter(exchange, chain).block();

        verify(redisTemplate, times(1)).expire(anyString(), eq(Duration.ofMinutes(20)));
    }

    @Test
    void shouldRunBeforeJwtFilter() {
        assertThat(filter.getOrder()).isLessThan(-1);
    }

    private RateLimitProperties.Endpoint endpointFor(String path, int maxRequests, int windowHours, int windowMinutes) {
        RateLimitProperties.Endpoint endpoint = new RateLimitProperties.Endpoint();
        endpoint.setPath(path);
        endpoint.setMaxRequests(maxRequests);
        endpoint.setWindowHours(windowHours);
        endpoint.setWindowMinutes(windowMinutes);
        return endpoint;
    }
}