package com.yukai.team.gatewayservice.security;

import com.yukai.team.gatewayservice.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Component
public class JwtTokenValidator {

    private static final String USER_ID_CLAIM = "userId";
    private static final String ROLES_CLAIM = "roles";
    private static final String INVALID_TOKEN_MESSAGE = "Invalid or expired token";

    private final JwtProperties jwtProperties;

    public JwtTokenValidator(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public AuthenticatedUser validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();
            Long userId = extractUserId(claims.get(USER_ID_CLAIM));
            List<String> roles = extractRoles(claims.get(ROLES_CLAIM));

            if (username == null || username.isBlank() || userId == null) {
                throw new JwtAuthenticationException(INVALID_TOKEN_MESSAGE);
            }

            return new AuthenticatedUser(userId, username, roles);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new JwtAuthenticationException(INVALID_TOKEN_MESSAGE, exception);
        }
    }

    private Long extractUserId(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.valueOf(text);
        }
        return null;
    }

    private List<String> extractRoles(Object value) {
        if (value instanceof Collection<?> values) {
            return values.stream()
                    .map(String::valueOf)
                    .map(this::normalizeRole)
                    .filter(role -> !role.isBlank())
                    .toList();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Arrays.stream(text.split(","))
                    .map(String::trim)
                    .map(this::normalizeRole)
                    .filter(role -> !role.isBlank())
                    .toList();
        }
        return List.of();
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String trimmedRole = role.trim();
        if (trimmedRole.startsWith("ROLE_")) {
            return trimmedRole.substring("ROLE_".length());
        }
        return trimmedRole;
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
