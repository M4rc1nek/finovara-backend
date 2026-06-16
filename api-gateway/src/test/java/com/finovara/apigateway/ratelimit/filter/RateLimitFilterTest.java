package com.finovara.apigateway.ratelimit.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finovara.apigateway.ratelimit.RateLimitMessage;
import com.finovara.contracts.exception.ErrorResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private RateLimitProperties properties;
    private ObjectMapper objectMapper = new ObjectMapper();
    private GatewayFilterChain chain = mock(GatewayFilterChain.class);

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        filter = new RateLimitFilter(properties, objectMapper);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void shouldPassThroughWhenPathNotConfigured() {
        properties.setEndpoints(List.of());

        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/anything").remoteAddress
                        (new InetSocketAddress("1.1.1.1", 0))
                .build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    @Test
    void shouldAllowRequestWhenUnderLimit() {
        RateLimitProperties.Endpoint endpoint = new RateLimitProperties.Endpoint();
        endpoint.setPath("/api/test");
        endpoint.setMaxRequests(3);
        endpoint.setWindowHours(1);
        properties.setEndpoints(List.of(endpoint));

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .remoteAddress(new InetSocketAddress("1.1.1.1", 0))
                        .build()
        );

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void shouldReturn429WhenLimitExceeded() throws Exception {
        RateLimitProperties.Endpoint endpoint = new RateLimitProperties.Endpoint();
        endpoint.setPath("/api/test");
        endpoint.setMaxRequests(1);
        endpoint.setWindowHours(1);
        properties.setEndpoints(List.of(endpoint));

        var first = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .remoteAddress(new InetSocketAddress("1.1.1.1", 0))
                        .build()
        );
        var second = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .remoteAddress(new InetSocketAddress("1.1.1.1", 0))
                        .build()
        );

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

        verify(chain, times(1)).filter(any()); // tylko pierwszy przeszedł
    }

    @Test
    void shouldTrackBucketsSeparatelyPerIp() {
        RateLimitProperties.Endpoint endpoint = new RateLimitProperties.Endpoint();
        endpoint.setPath("/api/test");
        endpoint.setMaxRequests(1);
        endpoint.setWindowHours(1);
        properties.setEndpoints(List.of(endpoint));

        var ip1 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .remoteAddress(new InetSocketAddress("1.1.1.1", 0))
                        .build()
        );
        var ip2 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .remoteAddress(new InetSocketAddress("1.1.1.1", 0))
                        .build()
        );
        var ip3 = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .remoteAddress(new InetSocketAddress("2.2.2.2", 0))
                        .build()
        );

        filter.filter(ip1, chain).block();
        filter.filter(ip2, chain).block();
        filter.filter(ip3, chain).block();

        assertThat(ip2.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(ip3.getResponse().getStatusCode()).isNull();
    }

    @Test
    void shouldMatchAntStyleWildcardPath() {
        RateLimitProperties.Endpoint endpoint = new RateLimitProperties.Endpoint();
        endpoint.setPath("/api/pdf/**");
        endpoint.setMaxRequests(5);
        endpoint.setWindowHours(1);
        properties.setEndpoints(List.of(endpoint));

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/pdf/reports/123")
                        .remoteAddress(new InetSocketAddress("1.1.1.1", 0))
                        .build()
        );

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    @Test
    void shouldUseXForwardedForInsteadOfRemoteAddress() {
        RateLimitProperties.Endpoint endpoint = new RateLimitProperties.Endpoint();
        endpoint.setPath("/api/test");
        endpoint.setMaxRequests(1);
        endpoint.setWindowHours(1);
        properties.setEndpoints(List.of(endpoint));

        var first = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .remoteAddress(new InetSocketAddress("9.9.9.9", 0))
                        .header("X-Forwarded-For", "5.5.5.5")
                        .build()
        );
        var second = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .remoteAddress(new InetSocketAddress("9.9.9.9", 0))
                        .header("X-Forwarded-For", "5.5.5.5")
                        .build()
        );

        filter.filter(first, chain).block();
        filter.filter(second, chain).block();

        assertThat(second.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void shouldRunBeforeJwtFilter() {
        assertThat(filter.getOrder()).isLessThan(-1);
    }
}