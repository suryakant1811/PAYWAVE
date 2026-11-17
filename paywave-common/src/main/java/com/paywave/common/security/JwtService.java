package com.paywave.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class JwtService {

    private final SecretKey key;
    private final long accessTokenMinutes;

    public JwtService(String secret, long accessTokenMinutes) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenMinutes = accessTokenMinutes <= 0 ? 15 : accessTokenMinutes;
    }

    public String createAccessToken(UUID userId, String email, Collection<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles == null ? List.of() : List.copyOf(roles))
                .claim("typ", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public long accessTokenSeconds() {
        return accessTokenMinutes * 60;
    }

    public AuthenticatedUser parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!"access".equals(claims.get("typ", String.class))) {
            throw new io.jsonwebtoken.JwtException("Not an access token");
        }
        Object rawRoles = claims.get("roles");
        Set<String> roles = new LinkedHashSet<>();
        if (rawRoles instanceof Collection<?> collection) {
            for (Object role : collection) {
                if (role != null) {
                    roles.add(role.toString());
                }
            }
        }
        return new AuthenticatedUser(UUID.fromString(claims.getSubject()), claims.get("email", String.class), Set.copyOf(roles));
    }
}
