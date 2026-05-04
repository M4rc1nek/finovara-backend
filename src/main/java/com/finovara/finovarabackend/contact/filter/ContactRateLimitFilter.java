package com.finovara.finovarabackend.contact.filter;

import com.finovara.finovarabackend.exception.tomanyrequest.TooManyRequests;
import com.finovara.finovarabackend.util.clientdata.ip.ClientIp;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ContactRateLimitFilter extends OncePerRequestFilter {

    private final ContactRateLimitProperties contactRateLimitProperties;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if (!request.getRequestURI().equals("/api/contact")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = ClientIp.getClientIpAddress(request);
        Bucket bucket = buckets.computeIfAbsent(ip, k -> createBucket());

        if (!bucket.tryConsume(1)) {
            throw new TooManyRequests("Too many requests. Try again in 1 hour.");
        }

        filterChain.doFilter(request, response);
    }

    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(
                        contactRateLimitProperties.getMaxRequests(),
                        Duration.ofHours(contactRateLimitProperties.getWindowHours())
                ))
                .build();
    }
}