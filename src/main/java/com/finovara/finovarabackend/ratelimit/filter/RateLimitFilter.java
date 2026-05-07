package com.finovara.finovarabackend.ratelimit.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finovara.finovarabackend.exception.ErrorResponseDto;
import com.finovara.finovarabackend.exception.serviceunavailable.ServiceUnavailableException;
import com.finovara.finovarabackend.ratelimit.RateLimitMessage;
import com.finovara.finovarabackend.util.clientdata.ip.ClientIp;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int TOO_MANY_REQUESTS = 429;
    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Optional<RateLimitProperties.Endpoint> endpoint = findEndpoint(request);

        if (endpoint.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = ClientIp.getClientIpAddress(request);
        RateLimitProperties.Endpoint rateLimitEndpoint = endpoint.get();
        String bucketKey = rateLimitEndpoint.getPath() + ":" + ip;
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(rateLimitEndpoint));

        if (!bucket.tryConsume(1)) {
            writeTooManyRequestsException(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Optional<RateLimitProperties.Endpoint> findEndpoint(HttpServletRequest request) {
        String requestUri = request.getRequestURI();

        return rateLimitProperties.getEndpoints().stream()
                .filter(endpoint -> StringUtils.hasText(endpoint.getPath()))
                .filter(endpoint -> pathMatcher.match(endpoint.getPath(), requestUri))
                .findFirst();
    }

    private Bucket createBucket(RateLimitProperties.Endpoint endpoint) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(endpoint.getMaxRequests())
                        .refillGreedy(endpoint.getMaxRequests(), Duration.ofHours(endpoint.getWindowHours()))
                        .build())
                .build();
    }

    private void writeTooManyRequestsException(HttpServletRequest request, HttpServletResponse response) {
        try {
            response.setStatus(TOO_MANY_REQUESTS);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            String message = RateLimitMessage.TRY_AGAIN_IN_1HOUR.label();

            ErrorResponseDto errorResponse = new ErrorResponseDto(
                    TOO_MANY_REQUESTS,
                    "Too Many Requests",
                    message,
                    request.getRequestURI()
            );

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            response.flushBuffer();

        } catch (IOException e) {
            throw new ServiceUnavailableException("Failed to throw Too many Request Exception", e);
        }
    }
}
