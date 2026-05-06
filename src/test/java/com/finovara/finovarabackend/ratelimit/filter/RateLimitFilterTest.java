package com.finovara.finovarabackend.ratelimit.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finovara.finovarabackend.exception.ErrorResponseDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;
    private RateLimitProperties rateLimitProperties;
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimitProperties = new RateLimitProperties();
        objectMapper = new ObjectMapper();
        rateLimitFilter = new RateLimitFilter(rateLimitProperties, objectMapper);
    }

    @Nested
    class EndpointMatching {

        @Test
        void shouldSkipFilteringWhenNoMatch() throws ServletException, IOException {
            rateLimitProperties.setEndpoints(List.of(createEndpoint("/api/limited", 1, 1)));
            when(request.getRequestURI()).thenReturn("/api/unlimited");

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(response);
        }

        @Test
        void shouldMatchAntPathPattern() throws ServletException, IOException {
            rateLimitProperties.setEndpoints(List.of(createEndpoint("/api/v1/**", 1, 1)));
            when(request.getRequestURI()).thenReturn("/api/v1/resource/123");
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    class RateLimitingLogic {
        private StringWriter stringWriter;
        private PrintWriter printWriter;

        @BeforeEach
        void setupEndpoints() throws IOException {
            stringWriter = new StringWriter();
            printWriter = new PrintWriter(stringWriter);

            rateLimitProperties.setEndpoints(List.of(createEndpoint("/api/test", 1, 1)));

            lenient().when(response.getWriter()).thenReturn(printWriter);
            lenient().when(request.getRequestURI()).thenReturn("/api/test");
        }

        @Test
        void shouldAllowRequest() throws ServletException, IOException {
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain, times(1)).doFilter(request, response);
            verify(response, never()).setStatus(429);
        }

        @Test
        void shouldBlockWhenQuotaExceeded() throws ServletException, IOException {
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");

            rateLimitFilter.doFilterInternal(request, response, filterChain);
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(response, atLeastOnce()).setStatus(429);
            verify(response, atLeastOnce()).setContentType(MediaType.APPLICATION_JSON_VALUE);

            ErrorResponseDto errorDto = objectMapper.readValue(stringWriter.toString(), ErrorResponseDto.class);

            assertThat(errorDto.status()).isEqualTo(429);
            assertThat(errorDto.error()).isEqualTo("Too Many Requests");
            assertThat(errorDto.message()).contains("Try again in 1 hour(s)");

            verify(filterChain, times(1)).doFilter(any(), any());
        }

        @Test
        void shouldHaveSeparateBucketsPerIp() throws ServletException, IOException {
            when(request.getRemoteAddr()).thenReturn("1.1.1.1");
            rateLimitFilter.doFilterInternal(request, response, filterChain);
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(429);
            clearInvocations(response, filterChain);

            when(request.getRemoteAddr()).thenReturn("2.2.2.2");
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(response, never()).setStatus(429);
        }
    }

    private RateLimitProperties.Endpoint createEndpoint(String path, int maxRequests, int hours) {
        RateLimitProperties.Endpoint endpoint = new RateLimitProperties.Endpoint();
        endpoint.setPath(path);
        endpoint.setMaxRequests(maxRequests);
        endpoint.setWindowHours(hours);
        return endpoint;
    }
}