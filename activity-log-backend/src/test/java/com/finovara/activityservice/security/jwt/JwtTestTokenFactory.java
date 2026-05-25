package com.finovara.activityservice.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

final class JwtTestTokenFactory {

    static final String SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    private JwtTestTokenFactory() {
    }

    static String token(Long userId, Date issuedAt, Date expiration) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("userId", userId)
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private static Key signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }
}
