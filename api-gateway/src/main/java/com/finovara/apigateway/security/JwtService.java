package com.finovara.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String USER_ID_CLAIM = "userId";
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userIdClaim = claims.get(USER_ID_CLAIM);
        if (userIdClaim instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(claims.getSubject());
    }

    public Mono<Boolean> isTokenValid(String token) {
        try {
            if (extractExpiration(token).before(new Date())) {
                return Mono.just(false);
            }
        } catch (JwtException | IllegalArgumentException e) {
            return Mono.just(false);
        }

        return reactiveRedisTemplate
                .hasKey(BLACKLIST_PREFIX + token)
                .map(blacklisted -> !blacklisted);
    }

    private Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }
}