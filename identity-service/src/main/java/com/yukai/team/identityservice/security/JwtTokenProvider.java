package com.yukai.team.identityservice.security;

import com.yukai.team.identityservice.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String USER_ID_CLAIM = "userId";
    private static final String ROLES_CLAIM = "roles";

    private final JwtProperties jwtProperties;

    public String generateAccessToken(Long userId, String username, List<String> roles) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(getExpirationSeconds());

        return Jwts.builder()
                .subject(username)
                .claim(USER_ID_CLAIM, userId)
                .claim(ROLES_CLAIM, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserId(String token) {
        Object userId = parseClaims(token).get(USER_ID_CLAIM);
        if (userId instanceof Number number) {
            return number.longValue();
        }
        if (userId instanceof String value) {
            return Long.valueOf(value);
        }
        return null;
    }

    public List<String> getRoles(String token) {
        Object roles = parseClaims(token).get(ROLES_CLAIM);
        if (roles instanceof List<?> values) {
            return values.stream()
                    .map(String::valueOf)
                    .toList();
        }
        return List.of();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            log.warn("JWT validation failed: {}", exception.getClass().getSimpleName());
            return false;
        }
    }

    public Long getExpirationSeconds() {
        return jwtProperties.getAccessTokenExpirationMinutes() * 60;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
